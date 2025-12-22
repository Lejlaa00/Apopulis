package com.example.apopulis.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.apopulis.R
import com.example.apopulis.databinding.FragmentLoginBinding
import androidx.lifecycle.lifecycleScope
import com.example.apopulis.model.LoginRequest
import com.example.apopulis.network.RetrofitInstance
import kotlinx.coroutines.launch
import com.example.apopulis.util.SessionManager

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        binding.btnLogin.setOnClickListener {
            validateLogin()
        }

        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment
            )
        }

        return binding.root
    }

    private fun validateLogin() {
        val username = binding.emailLayout.editText?.text.toString().trim()
        val password = binding.passwordLayout.editText?.text.toString().trim()

        var isValid = true

        // reset errors
        binding.emailLayout.error = null
        binding.passwordLayout.error = null

        // Username validation
        if (username.isEmpty()) {
            binding.emailLayout.error = "Username is required"
            isValid = false
        } else if (username.length < 3) {
            binding.emailLayout.error = "Username must be at least 3 characters"
            isValid = false
        }

        // Password validation
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            binding.passwordLayout.error = "Password must be at least 8 characters"
            isValid = false
        }

        if (!isValid) return

        // Backend login request
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.login(
                    LoginRequest(
                        username = username,
                        password = password
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()!!.token

                    // Save token
                    val sessionManager = SessionManager(requireContext())
                    sessionManager.saveToken(token)

                    Toast.makeText(
                        requireContext(),
                        "Login successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(
                        R.id.action_loginFragment_to_welcomeFragment
                    )

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Invalid username or password",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Network error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
