package com.ssafy.fitbox.controller;

import com.ssafy.fitbox.dto.request.CartItemRequestDto;
import com.ssafy.fitbox.dto.response.CartResponseDto;
import com.ssafy.fitbox.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
@Tag(name = "Cart", description = "장바구니 API")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(
            summary = "장바구니 조회",
            description = "사용자 ID를 기준으로 장바구니 목록을 조회합니다. 장바구니가 없으면 새 장바구니를 생성한 뒤 빈 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장바구니 조회 성공",
                    content = @Content(schema = @Schema(implementation = CartResponseDto.class))
            )
    })
    @GetMapping("/{userId}")
    public ResponseEntity<CartResponseDto> getCart(
            @Parameter(description = "사용자 PK ID", example = "1")
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @Operation(
            summary = "장바구니 아이템 추가",
            description = "사용자의 장바구니에 식단을 추가합니다. 현재 cart_item_table은 meal_id 기준으로 저장하므로 mealId와 quantity를 전송해야 합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "장바구니에 추가할 식단 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CartItemRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "완제품 식단 추가",
                                            value = """
                                                    {
                                                      "mealId": 1,
                                                      "quantity": 1
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "커스텀 식단 추가",
                                            value = """
                                                    {
                                                      "mealId": 11,
                                                      "quantity": 1
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장바구니 아이템 추가 성공",
                    content = @Content(schema = @Schema(implementation = CartResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "mealId 누락 또는 잘못된 요청",
                    content = @Content
            )
    })
    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponseDto> addCartItem(
            @Parameter(description = "사용자 PK ID", example = "1")
            @PathVariable Integer userId,

            @RequestBody CartItemRequestDto request
    ) {
        return ResponseEntity.ok(
                cartService.addCartItem(userId, request)
        );
    }

    @Operation(
            summary = "장바구니 아이템 삭제",
            description = "장바구니 아이템 ID를 기준으로 특정 아이템을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "장바구니 아이템 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "삭제할 아이템이 존재하지 않음", content = @Content)
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(
            @Parameter(description = "장바구니 아이템 ID", example = "1")
            @PathVariable Long cartItemId
    ) {
        cartService.deleteCartItem(cartItemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "장바구니 전체 비우기",
            description = "사용자 ID를 기준으로 해당 사용자의 장바구니 아이템을 모두 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "장바구니 비우기 성공")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "사용자 PK ID", example = "1")
            @PathVariable Integer userId
    ) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}