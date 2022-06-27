package com.arrowhead.arrownet

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class PageAdapter(fm : FragmentManager, private val contactsList: HashMap<String, String>) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return 1
    }

    override fun getItem(position: Int): Fragment {
        val bundle: Bundle = Bundle()

        return when(position) {
            0-> {
                val chatFragment = ChatsFragment()
                bundle.putSerializable("ContactsList", contactsList)
                chatFragment.arguments = bundle
                chatFragment
            }
            else-> {
                val chatFragment = ChatsFragment()
                bundle.putSerializable("ContactsList", contactsList)
                chatFragment.arguments = bundle
                chatFragment
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when(position) {
            0-> {
                return "Chats"
            }
        }
        return super.getPageTitle(position)
    }
}