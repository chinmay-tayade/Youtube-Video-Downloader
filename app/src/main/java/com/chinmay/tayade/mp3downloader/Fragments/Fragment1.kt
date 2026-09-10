package com.chinmay.tayade.mp3downloader.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.chinmay.tayade.mp3downloader.R

/**
 * Status card shown while a download is being prepared / is running.
 *
 * [setStatus] is safe to call before the view is created – the latest values are
 * cached and applied in [onViewCreated].
 */
class Fragment1 : Fragment() {

    private var messageView: TextView? = null
    private var progressBar: ProgressBar? = null

    private var pendingMessage: String? = null
    private var pendingFinished: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messageView = view.findViewById(R.id.message_status)
        progressBar = view.findViewById(R.id.rectangle_1)
        pendingMessage?.let { applyStatus(it, pendingFinished) }
    }

    override fun onDestroyView() {
        messageView = null
        progressBar = null
        super.onDestroyView()
    }

    /**
     * @param message   text to show in the status card
     * @param finished  when true the progress bar stops animating and fills
     */
    fun setStatus(message: String, finished: Boolean = false) {
        pendingMessage = message
        pendingFinished = finished
        if (isAdded) applyStatus(message, finished)
    }

    private fun applyStatus(message: String, finished: Boolean) {
        messageView?.text = message
        progressBar?.apply {
            if (finished) {
                isIndeterminate = false
                progress = 100
            } else {
                isIndeterminate = true
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = Fragment1()
    }
}
