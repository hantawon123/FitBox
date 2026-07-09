package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.ssafy.fitbox.R
import com.ssafy.fitbox.activity.MainActivity
import com.ssafy.fitbox.databinding.FragmentLoginBinding
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.notification.FcmTokenRegistrar
import com.ssafy.fitbox.repository.UserRepository
import com.ssafy.fitbox.util.SessionManager
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        initClickEvents()
        binding.root.post {
            bindAccountRecoveryClickEvents()
        }
    }

    private fun bindAccountRecoveryClickEvents() {
        binding.tvFindId.setOnClickListener {
            openAccountRecovery(AccountRecoveryFragment.newFindIdInstance())
        }

        binding.tvFindPassword.setOnClickListener {
            openAccountRecovery(AccountRecoveryFragment.newResetPasswordInstance())
        }
    }

    private fun openAccountRecovery(fragment: AccountRecoveryFragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun initClickEvents() {
        binding.btnLogin.setOnClickListener {
            loginWithIdAndPassword()
        }

        binding.tvGoRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.tvFindId.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "아이디 찾기 화면으로 이동합니다. (준비 중)",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.tvFindPassword.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "비밀번호 찾기 화면으로 이동합니다. (준비 중)",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.tvFindId.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountRecoveryFragment.newFindIdInstance())
                .addToBackStack(null)
                .commit()
        }

        binding.tvFindPassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountRecoveryFragment.newResetPasswordInstance())
                .addToBackStack(null)
                .commit()
        }

        binding.btnKakaoLogin.setOnClickListener {
            startKakaoLogin()
        }

        binding.btnNaverLogin.setOnClickListener {
            Toast.makeText(requireContext(), "네이버 로그인 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }

        binding.btnGoogleLogin.setOnClickListener {
            Toast.makeText(requireContext(), "구글 로그인 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginWithIdAndPassword() {
        val userId = binding.etUserId.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (userId.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "아이디와 비밀번호를 입력해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val loginRequest = User(
            userId = userId,
            password = password,
            name = "",
            phone = "",
            gender = "",
            age = 0,
            purpose = ""
        )

        lifecycleScope.launch {
            val result = userRepository.login(loginRequest)

            result.onSuccess { loggedInUser ->
                handleLoginSuccess(loggedInUser)
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "서버 통신 실패",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startKakaoLogin() {
        val context = requireContext()

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Toast.makeText(
                    context,
                    "카카오 로그인 실패: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val accessToken = token?.accessToken

                if (accessToken.isNullOrBlank()) {
                    Toast.makeText(
                        context,
                        "카카오 토큰을 받지 못했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    requestServerKakaoLogin(accessToken)
                }
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    UserApiClient.instance.loginWithKakaoAccount(
                        context = context,
                        callback = callback
                    )
                } else {
                    callback(token, null)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(
                context = context,
                callback = callback
            )
        }
    }

    private fun requestServerKakaoLogin(accessToken: String) {
        lifecycleScope.launch {
            val result = userRepository.loginWithKakao(accessToken)

            result.onSuccess { loggedInUser ->
                handleKakaoLoginSuccess(loggedInUser)
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "카카오 로그인 실패",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleLoginSuccess(loggedInUser: User) {
        sessionManager.createSession(loggedInUser)
        FcmTokenRegistrar.registerCurrentUser(requireContext().applicationContext)

        Toast.makeText(
            requireContext(),
            "${loggedInUser.name}님 환영합니다!",
            Toast.LENGTH_SHORT
        ).show()

        (requireActivity() as? MainActivity)?.onLoginSuccess(loggedInUser)
    }
    private fun handleKakaoLoginSuccess(loggedInUser: User) {
        sessionManager.createSession(loggedInUser)
        FcmTokenRegistrar.registerCurrentUser(requireContext().applicationContext)

        if (needsKakaoProfileEdit(loggedInUser)) {
            Toast.makeText(
                requireContext(),
                "카카오 계정으로 확인된 정보를 바탕으로 회원가입을 이어갑니다.",
                Toast.LENGTH_SHORT
            ).show()
            (requireActivity() as? MainActivity)?.onKakaoLoginNeedsRegistration()
        } else {
            Toast.makeText(
                requireContext(),
                "${loggedInUser.name}님 환영합니다!",
                Toast.LENGTH_SHORT
            ).show()
            (requireActivity() as? MainActivity)?.onKakaoLoginSuccessGoHome()
        }
    }

    private fun needsKakaoProfileEdit(user: User): Boolean {
        return user.userId.startsWith("kakao_") &&
                (
                        user.phone.startsWith("KAKAO_", ignoreCase = true) ||
                                user.phone.isBlank() ||
                                user.age <= 0 ||
                                user.height <= 0.0 ||
                                user.weight <= 0.0
                        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
