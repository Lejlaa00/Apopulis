package com.example.apopulis.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.apopulis.R
import com.example.apopulis.databinding.FragmentRegisterBinding
import com.example.apopulis.model.RegisterRequest
import com.example.apopulis.network.RetrofitInstance
import com.example.apopulis.util.SessionManager
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }

        return binding.root
    }

    private fun validateAndRegister() {
        val username = binding.usernameLayout.editText?.text.toString().trim()
        val email = binding.emailLayout.editText?.text.toString().trim()
        val password = binding.passwordLayout.editText?.text.toString().trim()

        var isValid = true

        binding.usernameLayout.error = null
        binding.emailLayout.error = null
        binding.passwordLayout.error = null

        // Username
        if (username.length < 3) {
            binding.usernameLayout.error = "Username must be at least 3 characters"
            isValid = false
        }

        // Email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Invalid email format"
            isValid = false
        }

        // Password
        if (password.length < 8 ||
            !password.any { it.isUpperCase() } ||
            !password.any { it.isDigit() } ||
            !password.any { "!@#$%^&*".contains(it) }
        ) {
            binding.passwordLayout.error =
                "Password must contain uppercase, number and special character"
            isValid = false
        }

        if (!isValid) return

        // Backend register
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.register(
                    RegisterRequest(username, email, password)
                )

                if (response.isSuccessful) {
                    val token = response.body()!!.token

                    // auto-login
                    SessionManager(requireContext()).saveToken(token)

                    Toast.makeText(
                        requireContext(),
                        "Registration successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(
                        R.id.action_registerFragment_to_welcomeFragment
                    )

                } else {
                    Toast.makeText(
                        requireContext(),
                        response.errorBody()?.string() ?: "Registration failed",
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
