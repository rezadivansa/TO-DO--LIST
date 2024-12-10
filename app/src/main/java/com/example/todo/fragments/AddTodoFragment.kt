package com.example.todo.fragments
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.DialogFragment
import com.example.todo.databinding.FragmentAddTodoBinding
import com.example.todo.utils.TodoData
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class AddTodoFragment : DialogFragment() {

    private lateinit var binding: FragmentAddTodoBinding
    private lateinit var listener: DialogNextButtonClickListener
    private val db = FirebaseFirestore.getInstance()
    var todoData: TodoData? = null
    val spinnerItems = listOf("Makan", "Minum", "Kerja","Tugas")

    fun setListener(listener: DialogNextButtonClickListener) {
        this.listener = listener
    }

    companion object {
        const val TAG = "AddTodoFragment"

        @JvmStatic
        fun newInstance(taskId: String, task: String, taskInfo: String, taskKategori: String) = AddTodoFragment().apply {
            arguments = Bundle().apply {
                putString("taskId", taskId)
                putString("task", task)
                putString("taskInfo", taskInfo)
                putString("taskKategori", taskKategori)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {


        binding = FragmentAddTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ArrayAdapter(
            requireContext(), // Context dari Fragment
            android.R.layout.simple_spinner_item, // Layout untuk item Spinner
            spinnerItems // Data yang akan ditampilkan di Spinner
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Layout dropdown
        binding.todoKategoriText.adapter = adapter // Set adapter ke Spinner
        if (arguments != null) {
            todoData = TodoData(
                arguments?.getString("taskId").toString(),
                arguments?.getString("task").toString(),
                arguments?.getString("taskInfo").toString(),
                arguments?.getString("taskKategori").toString()
            )
            binding.etTask.setText(todoData?.task)
            binding.etInfoTask.setText(todoData?.infoTask)

        }
        registerEvents()
    }

    private fun registerEvents() {
        binding.todoNextButton.setOnClickListener {
            val todoTask = binding.etTask.text.toString()
            val todoInfo = binding.etInfoTask.text.toString()
            val kategori = binding.todoKategoriText.toString()
            val hour = binding.tpTime.hour.toString()
            val minutes = binding.tpTime.minute.toString()
            val time = " Hour : $hour : Minute $minutes "
            if (todoTask.isNotEmpty()) {
                val newTodo = TodoData(
                    taskId = todoData?.taskId ?: System.currentTimeMillis().toString(),
                    task = todoTask,
                    infoTask = todoInfo,
                    kategoriTask = kategori,
                    time = time,
                    checked = false
                )
                saveToFirestore(newTodo)
            } else {
                Toast.makeText(context, "Please type some task", Toast.LENGTH_SHORT).show()
            }
        }
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun saveToFirestore(newTodo: TodoData) {
        binding.registerLoadingProgressBar.visibility = View.VISIBLE
        val todoCollection = db.collection("todos")
        todoCollection.document(newTodo.taskId).set(newTodo)
            .addOnSuccessListener {
                binding.registerLoadingProgressBar.visibility = View.GONE
                showNotification(newTodo.task)
                dismiss()
            }
            .addOnFailureListener { e ->
                binding.registerLoadingProgressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error saving Todo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showNotification(taskTitle: String) {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "todo_channel"
        val channelName = "Todo Notifications"

        // Membuat Notification Channel (dibutuhkan untuk Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Konfigurasi Notifikasi
        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Ikon bawaan Android
            .setContentTitle("Todo Added Successfully")
            .setContentText("Your task \"$taskTitle\" has been saved.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Notifikasi akan hilang ketika diklik
            .build()

        // Tampilkan Notifikasi
        notificationManager.notify(1, notification)
    }

    interface DialogNextButtonClickListener {
        fun onSaveTask(todo: String, todoEt: TextInputEditText)
        fun onUpdateTask(todoData: TodoData, todoEt: TextInputEditText)
    }
}