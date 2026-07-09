package com.ssafy.fitbox.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.fitbox.dto.DietReport
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.repository.IngredientRepository
import com.ssafy.fitbox.repository.UserRepository
import com.ssafy.fitbox.util.DietReportCache
import com.ssafy.fitbox.util.RecommendationHelper
import kotlinx.coroutines.launch

class MyPageViewModel : ViewModel() {

    private val ingredientRepository = IngredientRepository()
    private val userRepository = UserRepository()

    private val _aiReport = MutableLiveData<Result<DietReport>>()
    val aiReport: LiveData<Result<DietReport>> = _aiReport

    var isAnalyzed = false
    private var lastAnalysisKey: String? = null

    fun analyzeDiet(user: User, apiToken: String) {
        viewModelScope.launch {
            val result = ingredientRepository.getIngredients()

            result.onSuccess { ingredients ->
                // 🌟 [안전성 강화] 취향 정보 가져오기 실패 시 에러가 터지지 않도록 기본값 처리
                val userPreferenceInfo = try {
                    val prefResult = userRepository.getUserPreference(user.id)
                    val prefText = prefResult.getOrNull()
                    if (prefText.isNullOrBlank()) "없음" else prefText
                } catch (e: Exception) {
                    "없음"
                }

                val cacheKey = DietReportCache.createKey(user, ingredients, userPreferenceInfo)

                if (isAnalyzed && cacheKey == lastAnalysisKey) {
                    return@onSuccess
                }

                val cachedReport = DietReportCache.getReport(cacheKey)
                if (cachedReport != null) {
                    _aiReport.value = Result.success(cachedReport)
                    isAnalyzed = true
                    lastAnalysisKey = cacheKey
                    return@onSuccess
                }

                val reportResult = RecommendationHelper.getNutritionalAnalysis(
                    user = user,
                    ingredients = ingredients,
                    userPreference = userPreferenceInfo,
                    apiToken = apiToken
                )

                reportResult.onSuccess { report ->
                    DietReportCache.putReport(cacheKey, report)
                    lastAnalysisKey = cacheKey
                }

                _aiReport.value = reportResult
                isAnalyzed = true
            }.onFailure { exception ->
                _aiReport.value = Result.failure(exception)
            }
        }
    }

    fun resetAnalysis() {
        isAnalyzed = false
        lastAnalysisKey = null
        _aiReport.value = Result.success(DietReport(0.0, 0.0, 0.0, 0.0, 0, "", emptyList()))
    }

    fun invalidateAnalysis() {
        isAnalyzed = false
        lastAnalysisKey = null
    }
}
