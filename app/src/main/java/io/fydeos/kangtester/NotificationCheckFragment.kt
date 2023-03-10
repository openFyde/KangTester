package io.fydeos.kangtester

import android.Manifest
import android.app.Notification
import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.fydeos.kangtester.databinding.FragmentNotificationCheckBinding
import java.util.*
import kotlin.concurrent.fixedRateTimer


/**
 * A simple [Fragment] subclass.
 * Use the [NotificationCheckFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NotificationCheckFragment : Fragment() {

    private var gotPermission = false
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        run {
            gotPermission = isGranted
            notificationPermission()
        }
    }

    private lateinit var _binding: FragmentNotificationCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNotificationCheckBinding.inflate(inflater, container, false)
        return _binding.root
    }

    private fun notificationPermission() {
        if (gotPermission) {
            _binding.tvNoPermission.visibility = View.GONE
            _binding.lNotification.visibility = View.VISIBLE
        } else {
            _binding.tvNoPermission.visibility = View.VISIBLE
            _binding.lNotification.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.btnBasicNotification.setOnClickListener { postBasicNotification() }
        _binding.btnNotificationWithAction.setOnClickListener { postNotificationWithAction() }
        _binding.btnDelNotification.setOnClickListener {
            if (_progerssNotificationId == notificationId) {
                _progressTimer?.cancel()
                _progressTimer = null
            }
            NotificationManagerCompat.from(requireContext()).cancel(notificationId)
        }
        _binding.btnNotificationWithProgress.setOnClickListener { postNotificationWithProgressBar() }
        _binding.btnPostNotificationWithImage.setOnClickListener { postNotificationWithImage() }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                gotPermission = true
                notificationPermission()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            gotPermission = true
            notificationPermission()
        }
        createNotificationChannel()
    }

    private var _br: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, i: Intent?) {
            val act = i?.getStringExtra(KEY_TEXT_REPLY)
            _binding.tvNotificationComment.text =
                getString(R.string.notification_comment_received).format(act)
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(
            _br,
            IntentFilter().apply { this.addAction(NotificationClickedReceiver.RECV_ACTION) }
        )
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(_br)
    }

    private fun createNotificationChannel() {
        val name = "Test"
        val descriptionText = "Test Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager =
            getSystemService(requireContext(), NotificationManager::class.java)!!
        notificationManager.createNotificationChannel(mChannel)
    }

    private var notificationId = 0;

    private fun postBasicNotification() {
        val openUrlIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.notification_click_link)))
        val contentIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            openUrlIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.tablet_icon)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(requireContext())) {
            notificationId = (Math.random() * 10000).toInt()
            notify(notificationId, builder.build())
        }
    }

    private fun postNotificationWithImage() {
        val openUrlIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.notification_click_link)))
        val contentIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            openUrlIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val bitmap = BitmapFactory.decodeResource(
            requireContext().resources,
            R.drawable.fydetab
        )

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.tablet_icon)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_description))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setLargeIcon(bitmap)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null)
            )
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(requireContext())) {
            notificationId = (Math.random() * 10000).toInt()
            notify(notificationId, builder.build())
        }
    }

    // Key for the string that's delivered in the action's intent.
    private fun postNotificationWithAction() {
        notificationId = (Math.random() * 10000).toInt()
        val replyIntent =
            Intent(
                requireContext().applicationContext,
                NotificationClickedReceiver::class.java
            ).apply {
                action = BROADCAST_NAME
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_N_ACTION, KEY_ACTION_COMMENT)
            }
        val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel(getString(R.string.notification_type_prompt))
            build()
        }
        val m = if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val replyPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                requireActivity().applicationContext,
                notificationId,
                replyIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or m
            )

        val actionComment = NotificationCompat.Action.Builder(
            R.drawable.baseline_psychology_24, getString(R.string.notification_action_1),
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val dismissIntent =
            Intent(
                requireContext().applicationContext,
                NotificationClickedReceiver::class.java
            ).apply {
                action = BROADCAST_NAME
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_N_ACTION, KEY_ACTION_DISMISS)
            }

        val dismissPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                requireActivity().applicationContext,
                notificationId + 1,
                dismissIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or m
            )
        val actionDismiss = NotificationCompat.Action.Builder(
            R.drawable.baseline_psychology_24, getString(R.string.notification_action_2),
            dismissPendingIntent
        ).build()

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID).run {
            setSmallIcon(R.drawable.tablet_icon)
            setContentTitle(getString(R.string.notification_title))
            setContentText(getString(R.string.notification_description))
            priority = NotificationCompat.PRIORITY_DEFAULT
            addAction(actionComment)
            addAction(actionDismiss)
        }
        with(NotificationManagerCompat.from(requireContext())) {
            notify(notificationId, builder.build())
        }
    }

    private var _progerssNotificationId = 0
    private var _progressTimer: Timer? = null
    private fun postNotificationWithProgressBar() {
        notificationId = (Math.random() * 10000).toInt()
        val contentIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            Intent(),
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        _progerssNotificationId = notificationId
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID).apply {
            setContentTitle(getString(R.string.notification_title_2))
            setSmallIcon(R.drawable.baseline_psychology_24)
            setOnlyAlertOnce(true)
        }
        val PROGRESS_MAX = 100
        var PROGRESS_CURRENT = 0
        val handler = requireView().handler
        NotificationManagerCompat.from(requireContext()).apply {
            // Issue the initial notification with zero progress
            builder.setProgress(
                PROGRESS_MAX,
                PROGRESS_CURRENT,
                _binding.cbProgressIndeterminate.isChecked
            )
            notify(_progerssNotificationId, builder.build())

            _progressTimer?.cancel()
            _progressTimer = fixedRateTimer("progress", false, 0, 500) {
                handler.post {
                    if (PROGRESS_CURRENT >= PROGRESS_MAX) {
                        builder
                            .setContentText(getString(R.string.notification_content_3))
                            .setContentTitle(getString(R.string.notification_title_3))
                            .setProgress(0, 0, false)
                            .setContentIntent(contentIntent)
                            .setAutoCancel(true)
                        notify(_progerssNotificationId, builder.build())
                        _progressTimer?.cancel()
                    } else {
                        PROGRESS_CURRENT += 10
                        builder.setProgress(
                            PROGRESS_MAX,
                            PROGRESS_CURRENT,
                            _binding.cbProgressIndeterminate.isChecked
                        );
                        notify(_progerssNotificationId, builder.build());
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val CHANNEL_ID = "test_ch"
        const val EXTRA_N_ACTION = "io.fydeos.kangtester.action"
        const val KEY_ACTION_COMMENT = "comment"
        const val KEY_ACTION_DISMISS = "dismiss"
        const val BROADCAST_NAME = "io.fydeos.kangtester.NOTIFICATION_REPLIED"
    }
}