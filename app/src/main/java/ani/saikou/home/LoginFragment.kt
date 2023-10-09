package ani.saikou.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ani.saikou.R
import ani.saikou.connections.anilist.Anilist
import ani.saikou.databinding.FragmentLoginBinding
import ani.saikou.openLinkInBrowser

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.loginButton.setOnClickListener { Anilist.loginIntent(requireActivity()) }
        binding.loginDiscord.setOnClickListener { openLinkInBrowser(getString(R.string.discord)) }
        //binding.loginTelegram.setOnClickListener { openLinkInBrowser(getString(R.string.telegram)) }
        //binding.loginGithub.setOnClickListener { openLinkInBrowser(getString(R.string.github)) }
    }
}