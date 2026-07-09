import com.ssafy.fitbox.dto.CartItem

data class CartItemResponse(
    val cartItemId: Long,
    val mealId: Long,
    val name: String,
    val mealType: String,
    val price: Int,
    val calories: Double,
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double,
    val quantity: Int,
    val imageUrl: String?
) {
    fun toCartItem(): CartItem {
        return CartItem(
            id = cartItemId,
            mealId = mealId,
            name = name,
            mealType = mealType,
            calories = calories,
            carbohydrate = carbohydrate,
            protein = protein,
            fat = fat,
            price = price,
            quantity = quantity,
            imageUrl = imageUrl
        )
    }
}