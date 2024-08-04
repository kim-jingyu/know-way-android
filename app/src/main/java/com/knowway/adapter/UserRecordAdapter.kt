package com.knowway.adapter.user

import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.knowway.data.model.user.UserRecord
import com.knowway.databinding.ItemAdminRecordBinding
import com.knowway.R
import com.knowway.data.network.ApiClient
import com.knowway.data.network.user.UserApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserRecordAdapter(
    private val records: MutableList<UserRecord>, // Ensure this is a MutableList
    private val context: Context
) : RecyclerView.Adapter<UserRecordAdapter.RecordViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition = -1
    private val handler = Handler(Looper.getMainLooper())
    private var updateSeekBarTask: Runnable? = null

    inner class RecordViewHolder(val binding: ItemAdminRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Toggle expand/collapse on record title click
            binding.recordTitle.setOnClickListener {
                val record = records[adapterPosition]
                record.isExpanded = !record.isExpanded
                notifyItemChanged(adapterPosition)
            }

            // Play/Pause button functionality
            binding.btnPlayPause.setOnClickListener {
                val record = records[adapterPosition]
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_record_play)
                } else {
                    playMp3(record.recordUrl)
                    binding.btnPlayPause.setImageResource(R.drawable.ic_record_pause)
                }
            }

            // SeekBar listener for manual seek
            binding.musicSeekbar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Previous/Next buttons functionality
            binding.btnPrevious.setOnClickListener {
                mediaPlayer?.let {
                    val newPosition = it.currentPosition - 15000
                    it.seekTo(if (newPosition > 0) newPosition else 0)
                }
            }

            binding.btnNext.setOnClickListener {
                mediaPlayer?.let {
                    val newPosition = it.currentPosition + 15000
                    it.seekTo(if (newPosition < it.duration) newPosition else it.duration)
                }
            }

            // Action button functionality for delete confirmation
            binding.actionText.setOnClickListener {
                val record = records[adapterPosition]
                if (binding.actionText.text == "삭제") {
                    showDeleteConfirmationDialog(record, adapterPosition)
                }
            }
        }

        fun bind(record: UserRecord) {
            binding.recordTitle.text = record.recordTitle
            binding.musicControlLayout.visibility =
                if (record.isExpanded) View.VISIBLE else View.GONE
            binding.btnPlayPause.setImageResource(R.drawable.ic_record_play)

            // Background color based on expansion
            if (record.isExpanded) {
                binding.root.setBackgroundResource(R.drawable.record_item_background_expanded)
            } else {
                binding.root.setBackgroundResource(R.drawable.record_item_background)
            }

            // Set text for action button
            binding.actionText.text = "삭제"
            binding.actionText.setTextColor(ContextCompat.getColor(context, R.color.red))
            binding.actionText.visibility = View.VISIBLE

            if (mediaPlayer?.isPlaying == true && adapterPosition == currentPlayingPosition) {
                binding.musicSeekbar.max = mediaPlayer!!.duration
                binding.musicSeekbar.progress = mediaPlayer!!.currentPosition
                binding.btnPlayPause.setImageResource(R.drawable.ic_record_pause)
                startSeekBarUpdate()
            } else {
                stopSeekBarUpdate()
            }
        }

        private fun playMp3(audioFileUrl: String) {
            if (mediaPlayer != null && currentPlayingPosition != adapterPosition) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }

            currentPlayingPosition = adapterPosition

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
                try {
                    mediaPlayer?.apply {
                        setDataSource(audioFileUrl)
                        prepare()
                        start()
                        setVolume(1.0f, 1.0f)
                        setOnCompletionListener {
                            binding.btnPlayPause.setImageResource(R.drawable.ic_record_play)
                            binding.musicSeekbar.progress = 0
                            currentPlayingPosition = -1
                            stopSeekBarUpdate()
                        }
                    }
                    binding.musicSeekbar.max = mediaPlayer!!.duration
                    startSeekBarUpdate()
                } catch (e: Exception) {
                    Log.e("MediaPlayer", "Error playing audio file: $audioFileUrl", e)
                }
            } else {
                mediaPlayer?.start()
                mediaPlayer?.setVolume(1.0f, 1.0f) // Set to max volume
                startSeekBarUpdate()
            }
        }

        private fun startSeekBarUpdate() {
            updateSeekBarTask = object : Runnable {
                override fun run() {
                    if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                        binding.musicSeekbar.progress = mediaPlayer!!.currentPosition
                        handler.postDelayed(this, 1000)
                    }
                }
            }
            handler.post(updateSeekBarTask!!)
        }

        fun stopSeekBarUpdate() {
            updateSeekBarTask?.let { handler.removeCallbacks(it) }
        }

        private fun showDeleteConfirmationDialog(record: UserRecord, position: Int) {
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null)
            val confirmationMessage = dialogView.findViewById<TextView>(R.id.confirmation_message)
            val buttonCancel = dialogView.findViewById<Button>(R.id.button_cancel)
            val buttonConfirm = dialogView.findViewById<Button>(R.id.button_confirm)

            confirmationMessage.text = "해당 녹음을 삭제하시겠습니까?"

            val builder = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)

            val dialog = builder.create()

            buttonCancel.setOnClickListener {
                dialog.dismiss()
            }

            buttonConfirm.setOnClickListener {
                ApiClient.getClient().create(UserApiService::class.java)
                    .deleteUserRecord(record.recordId)
                    .enqueue(object : Callback<Boolean> {
                        override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                            if (response.isSuccessful && response.body() == true) {
                                records.removeAt(position)
                                notifyItemRemoved(position)
                                Toast.makeText(
                                    context,
                                    "삭제 완료: ${record.recordTitle}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Log.e("UserRecordAdapter", "Failed to delete user record")
                            }
                        }

                        override fun onFailure(call: Call<Boolean>, t: Throwable) {
                            Log.e("UserRecordAdapter", "Error deleting user record", t)
                        }
                    })

                dialog.dismiss()
            }

            dialog.show()
        }
    }

    // Correctly implement onCreateViewHolder here
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemAdminRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    override fun onViewRecycled(holder: RecordViewHolder) {
        super.onViewRecycled(holder)
        if (holder.adapterPosition == currentPlayingPosition) {
            holder.binding.musicSeekbar.progress = 0
            mediaPlayer?.release()
            mediaPlayer = null
            currentPlayingPosition = -1
            holder.stopSeekBarUpdate()
        }
    }
}
