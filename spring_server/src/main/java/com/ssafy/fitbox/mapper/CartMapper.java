package com.ssafy.fitbox.mapper;

import com.ssafy.fitbox.dto.request.CartItemRequestDto;
import com.ssafy.fitbox.dto.response.CartItemResponseDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {

    Long findCartIdByUserId(@Param("userId") Integer userId);

    void createCart(@Param("userId") Integer userId);

    void insertCartItem(
            @Param("cartId") Long cartId,
            @Param("request") CartItemRequestDto request
    );

    List<CartItemResponseDto> findCartItemsByCartId(
            @Param("cartId") Long cartId
    );

    void deleteCartItem(
            @Param("cartItemId") Long cartItemId
    );

    void clearCart(
            @Param("cartId") Long cartId
    );
    
    Long findCartItemIdByCartIdAndMealId(
            @Param("cartId") Long cartId,
            @Param("mealId") Long mealId
    );

    int increaseCartItemQuantity(
            @Param("cartItemId") Long cartItemId,
            @Param("quantity") Integer quantity
    );
    
}