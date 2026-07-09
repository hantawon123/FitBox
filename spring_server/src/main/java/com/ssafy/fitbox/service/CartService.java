package com.ssafy.fitbox.service;

import com.ssafy.fitbox.dto.request.CartItemRequestDto;
import com.ssafy.fitbox.dto.response.CartItemResponseDto;
import com.ssafy.fitbox.dto.response.CartResponseDto;
import com.ssafy.fitbox.mapper.CartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CartService {

    private final CartMapper cartMapper;

    public CartService(CartMapper cartMapper) {
        this.cartMapper = cartMapper;
    }

    public CartResponseDto getCart(Integer userId) {
        Long cartId = getOrCreateCartId(userId);
        return createCartResponse(userId, cartId);
    }

    public CartResponseDto addCartItem(
            Integer userId,
            CartItemRequestDto request
    ) {
        validateCartItemRequest(request);

        Long cartId = getOrCreateCartId(userId);

        int quantity = getValidQuantity(request);

        Long existingCartItemId = cartMapper.findCartItemIdByCartIdAndMealId(
                cartId,
                request.getMealId()
        );

        if (existingCartItemId != null) {
            cartMapper.increaseCartItemQuantity(
                    existingCartItemId,
                    quantity
            );
        } else {
            cartMapper.insertCartItem(
                    cartId,
                    request
            );
        }

        return createCartResponse(userId, cartId);
    }

    public void deleteCartItem(Long cartItemId) {
        cartMapper.deleteCartItem(cartItemId);
    }

    public void clearCart(Integer userId) {
        Long cartId = getOrCreateCartId(userId);
        cartMapper.clearCart(cartId);
    }

    private Long getOrCreateCartId(Integer userId) {
        Long cartId = cartMapper.findCartIdByUserId(userId);

        if (cartId == null) {
            cartMapper.createCart(userId);
            cartId = cartMapper.findCartIdByUserId(userId);
        }

        return cartId;
    }

    private CartResponseDto createCartResponse(
            Integer userId,
            Long cartId
    ) {
        List<CartItemResponseDto> items =
                cartMapper.findCartItemsByCartId(cartId);

        return new CartResponseDto(
                cartId,
                userId,
                items
        );
    }

    private void validateCartItemRequest(CartItemRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("장바구니 요청 정보가 없습니다.");
        }

        if (request.getMealId() == null) {
            throw new IllegalArgumentException("mealId가 필요합니다.");
        }
    }

    private int getValidQuantity(CartItemRequestDto request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            return 1;
        }

        return request.getQuantity();
    }
}