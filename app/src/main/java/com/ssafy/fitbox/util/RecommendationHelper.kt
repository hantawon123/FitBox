package com.ssafy.fitbox.util

import android.util.Log
import com.ssafy.fitbox.dto.ChatMessage
import com.ssafy.fitbox.dto.ChatRequest
import com.ssafy.fitbox.dto.DietItem
import com.ssafy.fitbox.dto.DietReport
import com.ssafy.fitbox.dto.Ingredient
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.network.RetrofitClient

object RecommendationHelper {
    private const val TAG = "RecHelper"

    private data class NutritionTarget(
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val guideline: String
    )

    suspend fun getNutritionalAnalysis(
        user: User,
        ingredients: List<Ingredient>,
        userPreference: String,
        apiToken: String
    ): Result<DietReport> {
        val bmr = if (user.gender == "M") {
            (10 * user.weight) + (6.25 * user.height) - (5 * user.age) + 5
        } else {
            (10 * user.weight) + (6.25 * user.height) - (5 * user.age) - 161
        }

        val activityMultiplier = when (user.activityLevel) {
            0, 1 -> 1.2
            2, 3 -> 1.375
            4, 5 -> 1.55
            else -> 1.725
        }
        val tdee = bmr * activityMultiplier
        val target = buildNutritionTarget(user, tdee)
        val allergyText = user.allergies.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "없음"
        val ingredientsNutritionString = ingredients
            .sortedWith(compareBy<Ingredient> { it.category.displayName }.thenBy { it.name })
            .joinToString("\n") {
                val per100Calories = it.calories * 100
                val per100Carbs = it.carbohydrate * 100
                val per100Protein = it.protein * 100
                val per100Fat = it.fat * 100
                val per100Price = it.price * 100
                "- ${it.name} / 분류:${it.category.displayName} / 100g 기준 ${String.format("%.1f", per100Calories)}kcal, 탄수 ${String.format("%.1f", per100Carbs)}g, 단백질 ${String.format("%.1f", per100Protein)}g, 지방 ${String.format("%.1f", per100Fat)}g, 가격 ${String.format("%,d", per100Price)}원"
            }

        // 🌟 [핵심 보완] 취향 텍스트가 실제로 존재할 때와 "없음"일 때의 AI 지시사항 분리
        val preferencePrompt = if (userPreference.isNotBlank() && userPreference != "없음") {
            """
            [★추가적인 개인 식단 취향 요약★]
            - ${userPreference}
            -> 사용자가 싫어한다고 한 음식이나 알레르기 유발 식재료는 [선택 가능한 재료]에 있더라도 절대 추천하지 마세요.
            -> 사용자가 좋아한다고 한 음식이 [선택 가능한 재료]에 존재하면, 영양 목표를 크게 해치지 않는 한 반드시 우선 포함하세요.
            -> 좋아하는 음식이 여러 개 존재하면 목적과 영양 목표에 가장 잘 맞는 재료를 1개 이상 포함하세요.
            -> 좋아하는 음식이 [선택 가능한 재료]에 없어서 넣지 못하는 경우에만 이유에 그 사실을 짧게 설명하세요.
            """
        } else {
            """
            [★추가적인 개인 식단 취향 요약★]
            - 없음
            (※ 위 취향 정보가 '없음'으로 되어 있으므로, 개인 취향 필터링 없이 오직 아래의 [사용자 기본 정보]와 목적에만 100% 집중해서 대중적이고 영양학적으로 가장 완벽한 조합을 추천해 주어야 해.)
            """
        }

        val detailPersonalizationRules = """
            [개인화 강화 규칙 - 반드시 따를 것]
            1. 추천 식단은 단순히 식사 목적(${user.purpose})만 보고 만들지 말고, 키/몸무게/활동레벨, 알레르기, 저장된 취향, 운동 종목, 생활 패턴, 구체 목표를 함께 반영하세요.
            2. 취향 요약에 "배드민턴", "축구", "러닝", "헬스", "바디프로필", "아침을 거름", "운동 후 회복식" 같은 디테일이 있으면 재료 선택과 이유에 반드시 반영하세요.
            3. 좋아하는 재료가 선택 가능 재료에 있으면 영양 목표를 크게 해치지 않는 한 우선 포함하세요. 싫어하는 재료와 알레르기 재료는 절대 포함하지 마세요.
            4. 고강도 운동이나 잦은 운동을 하는 사용자는 다이어트 목적이어도 근손실과 회복을 고려해 단백질과 탄수화물을 너무 낮추지 마세요.
            5. 이유 문장에는 왜 이 사용자에게 이 조합이 맞는지 짧게 설명하세요. 예: "배드민턴과 러닝처럼 소모량이 큰 운동을 하므로 감량 중에도 회복과 근손실 방지를 위해 단백질과 탄수화물을 함께 배치했습니다."
            6. reason은 2~3문장으로, 사용자의 구체 운동/생활 패턴/목표/취향 중 최소 1개 이상을 직접 언급하세요.
            7. reason에는 추천한 핵심 재료 2개 이상의 특징적인 영양상 장점이나 기대 효능을 함께 설명하세요. 예: 닭가슴살은 저지방 고단백이라 근손실 방지에 좋고, 현미밥은 복합 탄수화물이라 운동 에너지와 포만감 유지에 도움이 된다는 식으로 작성하세요.
            8. 재료 효능은 의학적 치료 효과처럼 과장하지 말고, 영양학적으로 자연스러운 표현(포만감, 단백질 보충, 회복, 식이섬유, 비타민/미네랄, 혈당 급상승 완화 등)으로 설명하세요.
        """.trimIndent()

        val prompt = """
            [지시] 
            당신은 데이터 기반의 수석 AI 영양사입니다. 아래 사용자 신체 정보, 목적별 영양 목표, 재료별 실제 영양 데이터를 모두 반영하여 한 끼 식단을 구성하세요.
            
            $preferencePrompt

            $detailPersonalizationRules
            
            [시스템 계산: 사용자 맞춤 1끼 목표치]
            - 추정 BMR: ${bmr.toInt()} kcal
            - 추정 TDEE: ${tdee.toInt()} kcal
            - 1끼 목표 칼로리: 약 ${target.calories.toInt()} kcal
            - 1끼 목표 탄수화물: 약 ${target.carbs.toInt()} g
            - 1끼 목표 단백질: 약 ${target.protein.toInt()} g
            - 1끼 목표 지방: 약 ${target.fat.toInt()} g
            ${target.guideline}

            [사용자 기본 정보]
            - 식사 목적: ${user.purpose}
            - 성별: ${user.gender}
            - 나이: ${user.age}세
            - 키: ${user.height}cm
            - 몸무게: ${user.weight}kg
            - 평소 운동 횟수: 일주일에 ${user.activityLevel}회
            - 알레르기: $allergyText

            [선택 가능한 재료와 실제 영양 정보]
            $ingredientsNutritionString

            [전문 추천 규칙 - 절대 준수]
            1. 재료는 위 목록에 있는 이름만 사용하세요. 이름을 바꾸거나 새로운 재료를 만들지 마세요.
            2. 개인 취향은 강한 제약입니다. 싫어하는 재료는 금지하고, 좋아하는 재료는 선택 가능하면 우선 포함하세요.
            3. 사용자가 좋아하는 단백질 재료가 선택 가능하면 두부, 닭가슴살 같은 대체 단백질보다 그 선호 재료를 먼저 사용하세요.
            4. 기본적으로 단백질 1개 이상, 탄수화물 1개 이상, 채소 1개 이상을 포함하세요. 단, 목적과 취향상 불가능하면 이유에 설명하세요.
            5. 소스는 필요할 때만 10~30g 범위로 제한하세요.
            6. 알레르기와 개인 취향에서 피해야 할 재료는 절대 추천하지 마세요.
            7. 칼로리는 목표치의 ±15%, 단백질은 목표치의 ±20% 안에 최대한 맞추세요.
            8. 모든 제공량은 반드시 10g 단위로 적으세요. 예: 137 불가, 140 가능
            9. 이유에는 사용자의 키, 몸무게, 목적, 취향을 근거로 왜 이 조합이 적합한지 전문적으로 설명하세요. 선호 재료를 넣었다면 그 점을 언급하고, 핵심 재료들의 영양상 장점도 함께 설명하세요.
            
            [출력 규칙 - 위반 시 시스템 오류 발생]
            1. 대괄호([])나 마크다운(**, -) 기호를 절대 사용하지 마세요.
            2. 제공량은 'g' 글자를 빼고 순수 숫자만 적으세요. (예: 150g -> 150)
            3. 아래 [모범 답안]의 형태와 100% 똑같이 답변하세요.

            [모범 답안 예시]
            재료:현미밥,150
            재료:닭가슴살,120
            재료:양상추,50
            이유:사용자님의 키와 몸무게, ${user.purpose} 목적, 목표 칼로리 ${target.calories.toInt()}kcal에 맞춰 단백질과 탄수화물, 채소를 균형 있게 배치했습니다. 닭가슴살은 저지방 고단백이라 근손실 방지에 도움이 되고, 고구마는 복합 탄수화물과 식이섬유가 있어 포만감과 운동 에너지 유지에 좋습니다.
        """.trimIndent()

        return runCatching {
            val request = ChatRequest(
                messages = listOf(
                    ChatMessage(
                        role = "system",
                        content = "너는 FitBox 앱의 데이터 기반 AI 영양사야. 재료별 영양 정보와 사용자 목적을 근거로 균형 잡힌 한 끼를 설계하고, 사용자가 준 출력 형식을 반드시 지켜야 해."
                    ),
                    ChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.2
            )
            val response = RetrofitClient.chatApi.sendMessage(apiToken, request)

            val body = response.body()
            if (!response.isSuccessful || body == null) {
                throw Exception("AI API 응답 오류: code=${response.code()}")
            }

            val aiResponse = body
                .choices
                .firstOrNull()
                ?.message
                ?.content
                ?.replace("```json", "")
                ?.replace("```", "")
                ?.trim()
                .orEmpty()

            if (aiResponse.isBlank()) {
                throw Exception("AI API 응답이 비어 있습니다.")
            }

            parseAndCalculate(aiResponse, ingredients)
        }
    }

    private fun parseAndCalculate(aiResponse: String, dbIngredients: List<Ingredient>): DietReport {
        val lines = aiResponse.lines()
        val recommendedItems = mutableListOf<DietItem>()
        var reason = "사용자 맞춤형 추천 식단입니다."

        var totalCal = 0.0
        var totalCarb = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalPrice = 0.0

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.contains("재료:")) {
                try {
                    val content = trimmedLine.substringAfter("재료:").trim()
                    val parts = content.split(",")
                    if (parts.size >= 2) {
                        val name = parts[0].replace(Regex("[\\[\\]]"), "").trim()
                        val amountStr = parts[1].replace(Regex("[^0-9.]"), "")
                        val amount = amountStr.toDoubleOrNull() ?: continue

                        val ingredient = dbIngredients.find { it.name.replace(" ", "") == name.replace(" ", "") }

                        if (ingredient != null) {
                            val itemCal = ingredient.calories * amount
                            recommendedItems.add(
                                DietItem(
                                    name = ingredient.name,
                                    amount = amount.toInt(),
                                    calories = itemCal,
                                    price = (ingredient.price * amount).toInt()
                                )
                            )
                            totalCal += itemCal
                            totalCarb += ingredient.carbohydrate * amount
                            totalProtein += ingredient.protein * amount
                            totalFat += ingredient.fat * amount
                            totalPrice += ingredient.price * amount
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "파싱 에러", e)
                }
            } else if (trimmedLine.contains("이유:")) {
                reason = trimmedLine.substringAfter("이유:").trim()
            }
        }

        if (recommendedItems.isEmpty()) {
            throw Exception("식단 매칭에 실패했습니다. AI 원문: $aiResponse")
        }

        return DietReport(
            totalCalories = totalCal,
            totalCarbs = totalCarb,
            totalProtein = totalProtein,
            totalFat = totalFat,
            totalPrice = totalPrice.toInt(),
            reason = reason,
            items = recommendedItems
        )
    }

    private fun buildNutritionTarget(user: User, tdee: Double): NutritionTarget {
        val calories = when (user.purpose) {
            "다이어트" -> ((tdee - 450) / 3).coerceAtLeast(350.0)
            "벌크업" -> (tdee + 450) / 3
            "유지어터" -> tdee / 3
            "운동 후 식사" -> (tdee / 3) + 100
            else -> tdee / 3
        }

        val protein = when (user.purpose) {
            "다이어트" -> (user.weight * 1.8) / 3
            "벌크업" -> (user.weight * 2.0) / 3
            "유지어터" -> (user.weight * 1.5) / 3
            "운동 후 식사" -> user.weight * 0.35
            else -> (user.weight * 1.5) / 3
        }.coerceAtLeast(18.0)

        val fatRatio = when (user.purpose) {
            "다이어트" -> 0.22
            "벌크업" -> 0.24
            "유지어터" -> 0.28
            "운동 후 식사" -> 0.15
            else -> 0.25
        }
        val fat = ((calories * fatRatio) / 9).coerceAtLeast(8.0)
        val carbs = ((calories - (protein * 4) - (fat * 9)) / 4).coerceAtLeast(20.0)

        val guideline = when (user.purpose) {
            "다이어트" -> """
                - 목적 기준: 감량을 위해 과한 열량은 피하되 포만감과 근손실 방지를 우선합니다.
                - 구성 방향: 고단백 재료, 채소, 적당한 복합탄수화물 중심으로 구성하고 지방과 소스는 절제하세요.
            """.trimIndent()
            "벌크업" -> """
                - 목적 기준: 근성장을 위해 충분한 열량과 탄수화물, 단백질을 함께 확보합니다.
                - 구성 방향: 단백질 재료를 넉넉히 두고, 운동 에너지를 위한 탄수화물 베이스를 반드시 포함하세요.
            """.trimIndent()
            "유지어터" -> """
                - 목적 기준: 체중 유지와 지속 가능성을 위해 탄수화물, 단백질, 지방의 균형을 우선합니다.
                - 구성 방향: 한쪽 영양소에 치우치지 않게 단백질, 탄수화물, 채소를 고르게 배치하세요.
            """.trimIndent()
            "운동 후 식사" -> """
                - 목적 기준: 운동 후 회복을 위해 단백질과 글리코겐 보충용 탄수화물을 우선합니다.
                - 구성 방향: 소화 부담이 큰 지방과 소스는 줄이고, 단백질과 탄수화물을 중심으로 구성하세요.
            """.trimIndent()
            else -> """
                - 목적 기준: 목적이 명확하지 않으므로 균형 잡힌 일반식을 우선합니다.
                - 구성 방향: 단백질, 탄수화물, 채소를 고르게 배치하세요.
            """.trimIndent()
        }

        return NutritionTarget(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            guideline = guideline
        )
    }
}
