package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.fitbox.adapter.SubscriptionHistoryAdapter
import com.ssafy.fitbox.databinding.FragmentSubscriptionHistoryBinding
import com.ssafy.fitbox.network.response.SubscriptionResponse
import com.ssafy.fitbox.network.response.SubscriptionTemplateResponse
import com.ssafy.fitbox.util.FavoriteMealStore
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SubscriptionHistoryFragment : Fragment() {
    private var _binding: FragmentSubscriptionHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrderViewModel by viewModels()
    private var currentSubscriptions: List<SubscriptionResponse> = emptyList()
    private val adapter = SubscriptionHistoryAdapter(
        onCancel = { subscription ->
            val userId = SessionManager(requireContext()).getUser()?.id
                ?: return@SubscriptionHistoryAdapter
            AlertDialog.Builder(requireContext())
                .setTitle("구독 취소")
                .setMessage("정기구독을 취소하시겠습니까?\n취소 후에는 다음 주문이 생성되지 않습니다.")
                .setPositiveButton("취소하기") { _, _ ->
                    viewModel.cancelSubscription(subscription.subscriptionGroupId, userId)
                }
                .setNegativeButton("유지하기", null)
                .show()
        },
        onResubscribe = { subscription ->
            parentFragmentManager.beginTransaction()
                .replace(
                    com.ssafy.fitbox.R.id.main_container,
                    SubscriptionOrderFragment.newResubscribeInstance(subscription)
                )
                .addToBackStack(null)
                .commit()
        },
        onAddFavorite = { subscription ->
            addSubscriptionMenusToFavorite(subscription)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnShowSubscriptionCalendar.setOnClickListener {
            showMarkedSubscriptionCalendarDialog()
        }

        binding.btnSubscriptionEmptyAction.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    com.ssafy.fitbox.R.id.main_container,
                    SubscriptionOrderFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        binding.rvSubscriptions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSubscriptions.adapter = adapter

        viewModel.userSubscriptions.observe(viewLifecycleOwner) { subscriptions ->
            currentSubscriptions = subscriptions
            adapter.submitList(subscriptions)
            binding.layoutSubscriptionEmpty.visibility =
                if (subscriptions.isEmpty()) View.VISIBLE else View.GONE
            binding.rvSubscriptions.visibility =
                if (subscriptions.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.orderListErrorMessage.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
        viewModel.cancelSubscriptionResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "구독을 취소했습니다.", Toast.LENGTH_SHORT).show()
                SessionManager(requireContext()).getUser()?.id?.let(viewModel::getUserSubscriptions)
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    it.message ?: "구독 취소에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val userId = SessionManager(requireContext()).getUser()?.id
        if (userId == null) {
            binding.layoutSubscriptionEmpty.visibility = View.VISIBLE
        } else {
            viewModel.getUserSubscriptions(userId)
        }
    }

    private fun showSubscriptionCalendarDialog() {
        if (currentSubscriptions.isEmpty()) {
            Toast.makeText(requireContext(), "확인할 구독 일정이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }
        val calendarView = CalendarView(requireContext())
        val detailText = TextView(requireContext()).apply {
            setTextColor(resources.getColor(com.ssafy.fitbox.R.color.mypage_text, null))
            textSize = 14f
            setPadding(0, 16, 0, 0)
            setLineSpacing(6f, 1f)
        }

        fun bindDate(year: Int, month: Int, dayOfMonth: Int) {
            val date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val schedules = getSubscriptionSchedulesForDate(date)
            detailText.text = if (schedules.isEmpty()) {
                "$date\n이 날짜에 예정된 구독 식단이 없습니다."
            } else {
                "$date\n" + schedules.joinToString("\n") { "· $it" }
            }
        }

        val today = Calendar.getInstance()
        bindDate(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            bindDate(year, month, dayOfMonth)
        }

        container.addView(calendarView)
        container.addView(detailText)

        AlertDialog.Builder(requireContext())
            .setTitle("구독 일정 확인")
            .setMessage("날짜를 선택하면 해당 날짜의 반복 구독 식단을 확인할 수 있어요.")
            .setView(container)
            .setPositiveButton("확인", null)
            .show()
    }

    private fun showMarkedSubscriptionCalendarDialog() {
        if (currentSubscriptions.isEmpty()) {
            Toast.makeText(requireContext(), "확인할 구독 일정이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), 0)
        }
        val guideText = TextView(requireContext()).apply {
            text = "초록색으로 표시된 날짜에 구독 식단이 있어요."
            setTextColor(resources.getColor(com.ssafy.fitbox.R.color.mypage_text_secondary, null))
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        }
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(12))
        }
        val btnPrevMonth = createCalendarNavButton("‹")
        val btnNextMonth = createCalendarNavButton("›")
        val monthTitle = TextView(requireContext()).apply {
            gravity = Gravity.CENTER
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(resources.getColor(com.ssafy.fitbox.R.color.mypage_text, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val calendarGrid = GridLayout(requireContext()).apply {
            columnCount = 7
        }
        val detailText = TextView(requireContext()).apply {
            setTextColor(resources.getColor(com.ssafy.fitbox.R.color.mypage_text, null))
            textSize = 14f
            setPadding(0, dp(16), 0, 0)
            setLineSpacing(6f, 1f)
        }

        val selectedDate = Calendar.getInstance()
        val displayMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }

        fun renderCalendar() {
            renderSubscriptionCalendar(
                displayMonth = displayMonth,
                selectedDate = selectedDate,
                calendarGrid = calendarGrid,
                monthTitle = monthTitle,
                detailText = detailText,
                onDateSelected = { picked ->
                    selectedDate.timeInMillis = picked.timeInMillis
                    displayMonth.set(Calendar.YEAR, picked.get(Calendar.YEAR))
                    displayMonth.set(Calendar.MONTH, picked.get(Calendar.MONTH))
                    displayMonth.set(Calendar.DAY_OF_MONTH, 1)
                    renderCalendar()
                }
            )
        }

        btnPrevMonth.setOnClickListener {
            displayMonth.add(Calendar.MONTH, -1)
            renderCalendar()
        }
        btnNextMonth.setOnClickListener {
            displayMonth.add(Calendar.MONTH, 1)
            renderCalendar()
        }

        header.addView(btnPrevMonth)
        header.addView(monthTitle)
        header.addView(btnNextMonth)

        container.addView(guideText)
        container.addView(header)
        container.addView(calendarGrid)
        container.addView(detailText)
        renderCalendar()

        AlertDialog.Builder(requireContext())
            .setTitle("구독 일정 확인")
            .setView(container)
            .setPositiveButton("확인", null)
            .show()
    }

    private fun renderSubscriptionCalendar(
        displayMonth: Calendar,
        selectedDate: Calendar,
        calendarGrid: GridLayout,
        monthTitle: TextView,
        detailText: TextView,
        onDateSelected: (Calendar) -> Unit
    ) {
        calendarGrid.removeAllViews()
        monthTitle.text = String.format(
            Locale.getDefault(),
            "%04d년 %d월",
            displayMonth.get(Calendar.YEAR),
            displayMonth.get(Calendar.MONTH) + 1
        )

        listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
            calendarGrid.addView(
                TextView(requireContext()).apply {
                    text = label
                    gravity = Gravity.CENTER
                    textSize = 13f
                    setTextColor(resources.getColor(com.ssafy.fitbox.R.color.mypage_text_secondary, null))
                    layoutParams = createCalendarCellLayoutParams(dp(28))
                }
            )
        }

        val firstDay = displayMonth.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOffset = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val maxDay = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)

        repeat(firstDayOffset) {
            calendarGrid.addView(View(requireContext()).apply {
                layoutParams = createCalendarCellLayoutParams(dp(54))
            })
        }

        for (day in 1..maxDay) {
            val date = displayMonth.clone() as Calendar
            date.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = formatCalendarToDateString(date)
            val schedules = getSubscriptionSchedulesForDate(dateKey)
            calendarGrid.addView(
                createSubscriptionCalendarDayView(
                    date = date,
                    schedules = schedules,
                    isSelected = isSameDate(date, selectedDate),
                    onClick = onDateSelected
                )
            )
        }

        bindCalendarDetail(selectedDate, detailText)
    }

    private fun createSubscriptionCalendarDayView(
        date: Calendar,
        schedules: List<String>,
        isSelected: Boolean,
        onClick: (Calendar) -> Unit
    ): View {
        val hasSchedule = schedules.isNotEmpty()
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = createSubscriptionCalendarDayBackground(isSelected, hasSchedule)
            layoutParams = createCalendarCellLayoutParams(dp(54)).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick(date.clone() as Calendar) }

            addView(TextView(requireContext()).apply {
                text = date.get(Calendar.DAY_OF_MONTH).toString()
                gravity = Gravity.CENTER
                textSize = 15f
                typeface = if (hasSchedule || isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(
                    resources.getColor(
                        when {
                            isSelected -> android.R.color.white
                            hasSchedule -> com.ssafy.fitbox.R.color.fit_primary_dark
                            else -> com.ssafy.fitbox.R.color.mypage_text
                        },
                        null
                    )
                )
            })

            addView(TextView(requireContext()).apply {
                text = if (hasSchedule) "${schedules.size}개" else ""
                gravity = Gravity.CENTER
                textSize = 10f
                setTextColor(
                    resources.getColor(
                        if (isSelected) android.R.color.white else com.ssafy.fitbox.R.color.fit_primary,
                        null
                    )
                )
            })
        }
    }

    private fun createCalendarCellLayoutParams(height: Int): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = 0
            this.height = height
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
    }

    private fun createSubscriptionCalendarDayBackground(
        isSelected: Boolean,
        hasSchedule: Boolean
    ): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            when {
                isSelected -> setColor(resources.getColor(com.ssafy.fitbox.R.color.mypage_primary, null))
                hasSchedule -> {
                    setColor(resources.getColor(com.ssafy.fitbox.R.color.fit_primary_container, null))
                    setStroke(dp(1), resources.getColor(com.ssafy.fitbox.R.color.fit_primary, null))
                }
                else -> setColor(resources.getColor(android.R.color.transparent, null))
            }
        }
    }

    private fun createCalendarNavButton(label: String): TextView {
        return TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(resources.getColor(com.ssafy.fitbox.R.color.mypage_text, null))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            isClickable = true
            isFocusable = true
        }
    }

    private fun bindCalendarDetail(date: Calendar, detailText: TextView) {
        val dateKey = formatCalendarToDateString(date)
        val schedules = getSubscriptionSchedulesForDate(dateKey)
        detailText.text = if (schedules.isEmpty()) {
            "$dateKey\n선택한 날짜에 예정된 구독 식단이 없습니다."
        } else {
            "$dateKey\n" + schedules.joinToString("\n") { "· $it" }
        }
    }

    private fun isSameDate(left: Calendar, right: Calendar): Boolean {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.MONTH) == right.get(Calendar.MONTH)
                && left.get(Calendar.DAY_OF_MONTH) == right.get(Calendar.DAY_OF_MONTH)
    }

    private fun formatCalendarToDateString(calendar: Calendar): String {
        return String.format(
            Locale.getDefault(),
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun getSubscriptionSchedulesForDate(date: String): List<String> {
        val target = parseDate(date) ?: return emptyList()
        val dayOfWeek = target.get(Calendar.DAY_OF_WEEK)
        val weekOfMonth = ((target.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1

        return currentSubscriptions
            .filter { it.status != "CANCELED" }
            .filter { subscription ->
                val start = parseDate(subscription.subscriptionStartDate) ?: return@filter false
                val end = subscription.subscriptionEndDate?.let(::parseDate)
                !target.before(start) && (end == null || !target.after(end))
            }
            .flatMap { subscription ->
                subscription.templates.orEmpty()
                    .filter { it.weekOfMonth == weekOfMonth && it.dayOfWeek == dayOfWeek }
                    .map {
                        "${it.mealName} ${it.quantity}개 · ${it.weekOfMonth}주차 ${it.dayOfWeekText}"
                    }
            }
    }

    private fun parseDate(date: String): Calendar? {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsed = formatter.parse(date) ?: return null
            Calendar.getInstance().apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun addSubscriptionMenusToFavorite(subscription: SubscriptionResponse) {
        val userId = SessionManager(requireContext()).getUser()?.id
        if (userId == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val templates = subscription.templates.orEmpty()
            .distinctBy { it.mealId }
            .sortedWith(compareBy({ it.weekOfMonth }, { it.dayOfWeek }))

        when (templates.size) {
            0 -> {
                Toast.makeText(requireContext(), "즐겨찾기에 추가할 구독 메뉴가 없습니다.", Toast.LENGTH_SHORT).show()
            }

            1 -> {
                addTemplatesToFavorite(userId, templates)
            }

            else -> {
                showSubscriptionFavoritePicker(userId, templates)
            }
        }
    }

    private fun showSubscriptionFavoritePicker(
        userId: Int,
        templates: List<SubscriptionTemplateResponse>
    ) {
        val checkedItems = BooleanArray(templates.size) { true }
        val labels = templates.map {
            "${it.weekOfMonth}주차 ${it.dayOfWeekText} · ${it.mealName} ${it.quantity}개"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("즐겨찾기에 넣을 구독 메뉴 선택")
            .setMultiChoiceItems(labels, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("추가") { _, _ ->
                val selected = templates.filterIndexed { index, _ -> checkedItems[index] }
                if (selected.isEmpty()) {
                    Toast.makeText(requireContext(), "메뉴를 하나 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    addTemplatesToFavorite(userId, selected)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun addTemplatesToFavorite(
        userId: Int,
        templates: List<SubscriptionTemplateResponse>
    ) {
        FavoriteMealStore.initialize(requireContext())
        val addedCount = templates.count {
            FavoriteMealStore.addFromSubscriptionTemplate(userId, it)
        }
        val duplicatedCount = templates.size - addedCount
        val message = when {
            addedCount > 0 && duplicatedCount > 0 -> {
                "${addedCount}개 구독 식단을 즐겨찾기에 추가했습니다.\n${duplicatedCount}개는 이미 추가된 메뉴입니다."
            }

            addedCount > 0 -> {
                "${addedCount}개 구독 식단을 즐겨찾기에 추가했습니다."
            }

            else -> {
                "이미 즐겨찾기에 추가된 메뉴입니다."
            }
        }

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
