package com.example.apopulis.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.apopulis.R
import com.example.apopulis.util.SessionManager

class WelcomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

        view.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_login)
        }

        view.findViewById<Button>(R.id.btnRegister).setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_register)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())

        if (sessionManager.isLoggedIn()) {
            findNavController().navigate(
                R.id.action_welcome_to_login
            )
        }
    }
}
