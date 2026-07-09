package com.ssafy.fitbox.dto

enum class MealCategory(
    val displayName: String,
    val description: String
) {
    ALL(
        displayName = "전체 식단",
        description = "판매 중인 완제품 식단을 모두 확인해보세요."
    ),
    DIET(
        displayName = "다이어트",
        description = "부담을 낮추고 영양 균형을 챙긴 식단이에요."
    ),
    BULKING(
        displayName = "벌크업",
        description = "단백질과 에너지를 충분히 채울 수 있는 식단이에요."
    ),
    MAINTAIN(
        displayName = "유지어터",
        description = "꾸준한 체중 관리에 어울리는 균형 식단이에요."
    ),
    POST_WORKOUT(
        displayName = "운동 후 식사",
        description = "운동 후 단백질 보충과 회복을 돕는 식단이에요."
    ),
    SALAD(
        displayName = "샐러드",
        description = "신선하고 가볍게 즐길 수 있는 샐러드 식단이에요."
    ),
    SIMPLE(
        displayName = "간편식",
        description = "빠르고 간편하게 챙길 수 있는 식단이에요."
    );

    fun matches(product: Product): Boolean {
        val normalizedName = product.name.lowercase()
        return when (this) {
            ALL -> true
            DIET ->
                normalizedName.contains("다이어트") ||
                    normalizedName.contains("닭가슴") ||
                    normalizedName.contains("저칼로리") ||
                    product.calories <= 450

            BULKING ->
                normalizedName.contains("벌크") ||
                    normalizedName.contains("고단백") ||
                    product.protein >= 30

            MAINTAIN ->
                normalizedName.contains("균형") ||
                    normalizedName.contains("점심") ||
                    normalizedName.contains("덮밥") ||
                    normalizedName.contains("두부") ||
                    (product.calories in 400.0..650.0 && product.protein in 15.0..35.0)

            POST_WORKOUT ->
                normalizedName.contains("닭가슴") ||
                    normalizedName.contains("소고기") ||
                    normalizedName.contains("두부") ||
                    normalizedName.contains("고단백") ||
                    product.protein >= 25

            SALAD ->
                normalizedName.contains("샐러드") ||
                    normalizedName.contains("salad")

            SIMPLE ->
                normalizedName.contains("간편") ||
                    normalizedName.contains("도시락") ||
                    normalizedName.contains("box")
        }
    }

    companion object {
        fun fromName(name: String?): MealCategory {
            return entries.firstOrNull { it.name == name } ?: ALL
        }
    }
}
