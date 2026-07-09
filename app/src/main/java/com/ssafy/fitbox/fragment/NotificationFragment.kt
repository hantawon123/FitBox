package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.fitbox.adapter.NotificationAdapter
import com.ssafy.fitbox.databinding.FragmentNotificationBinding
import com.ssafy.fitbox.repository.NotificationRepository
import com.ssafy.fitbox.util.SessionManager
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val repository = NotificationRepository()
    private val adapter = NotificationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter
        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadNotifications()
        }
    }

    private fun loadNotifications() {
        val userId = SessionManager(requireContext()).getUser()?.id ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val notifications = repository.getNotifications(userId).getOrDefault(emptyList())
            adapter.submitList(notifications)
            repository.markAllAsRead(userId)
            val currentBinding = _binding ?: return@launch
            currentBinding.layoutNotificationEmpty.visibility =
                if (notifications.isEmpty()) View.VISIBLE else View.GONE
            currentBinding.rvNotifications.visibility =
                if (notifications.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
