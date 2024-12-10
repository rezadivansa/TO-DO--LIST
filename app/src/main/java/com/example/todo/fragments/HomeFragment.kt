package com.example.todo.fragments

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todo.R
import com.example.todo.databinding.FragmentHomeBinding
import com.example.todo.utils.TodoAdapter
import com.example.todo.utils.TodoData
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore


class HomeFragment : Fragment(), AddTodoFragment.DialogNextButtonClickListener,
    TodoAdapter.TodoAdapterClickInterface, LogOutPopupFragment.LogoutInterface {

    private lateinit var auth : FirebaseAuth
    private lateinit var binding : FragmentHomeBinding
    private lateinit var navControl : NavController
    private var popUpFragment : AddTodoFragment? = null
    private lateinit var adapter : TodoAdapter
    private lateinit var mList : MutableList<TodoData>
    private lateinit var logoutPopup : LogOutPopupFragment
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        getDataFromFirebase()
        registerEvents()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getDataFromFirebase() {
        firestore.collection("todos")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    mList.clear()
                    for (document in snapshots) {
                        val todoTask = document.toObject(TodoData::class.java)
                        mList.add(todoTask)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }



    private fun registerEvents() {
        binding.addHomeButton.setOnClickListener{
            if(popUpFragment != null)
                childFragmentManager.beginTransaction().remove(popUpFragment!!).commit()
            popUpFragment = AddTodoFragment()
            popUpFragment!!.setListener(this)
            popUpFragment!!.show(
                childFragmentManager,
                AddTodoFragment.TAG
            )
        }
        binding.backButton.setOnClickListener {
            logoutPopup = LogOutPopupFragment()
            logoutPopup.setLogoutListener(this)
            logoutPopup.show(
                childFragmentManager,
                "LogoutPopup"
            )
        }
    }

    private fun init(view: View) {
        auth = FirebaseAuth.getInstance()
        navControl = Navigation.findNavController(view)
        firestore = FirebaseFirestore.getInstance() // Inisialisasi Firestore
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        mList = mutableListOf()
        adapter = TodoAdapter(mList)
        adapter.setListener(this)
        binding.recyclerView.adapter = adapter
    }

    override fun onSaveTask(todo: String, todoEt: TextInputEditText) {
        val collectionPath = "todos" // Path koleksi yang digunakan
        val newTask = hashMapOf(
            "taskId" to firestore.collection(collectionPath).document().id,
            "task" to todo,
            "infoTask" to "Deskripsi tugas", // Tambahkan informasi default
            "kategoriTask" to "Kategori", // Tambahkan kategori default
            "time" to System.currentTimeMillis().toString(),
            "checked" to false
        )

        firestore.collection(collectionPath)
            .add(newTask)
            .addOnSuccessListener {
                Toast.makeText(context, "Task saved successfully", Toast.LENGTH_SHORT).show()
                todoEt.text?.clear()
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        popUpFragment?.dismiss()
    }

    override fun onUpdateTask(todoData: TodoData, todoEt: TextInputEditText) {
        val updatedData = mapOf(
            "task" to todoData.task,
            "infoTask" to todoData.infoTask,
            "kategoriTask" to todoData.kategoriTask,
            "time" to todoData.time,
            "checked" to todoData.checked
        )

        firestore.collection("todos").document(todoData.taskId)
            .update(updatedData)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Task Updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, it.exception?.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        todoEt.text?.clear()
        popUpFragment?.dismiss()
    }

    override fun onDeleteTaskButtonClicked(todoData: TodoData) {
        firestore.collection("todos").document(todoData.taskId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    override fun onEditTaskButtonClicked(todoData: TodoData) {
        if(popUpFragment != null)
            childFragmentManager.beginTransaction().remove(popUpFragment!!).commit()

        popUpFragment = AddTodoFragment.newInstance(todoData.taskId, todoData.task, todoData.infoTask, todoData.kategoriTask)
        popUpFragment!!.setListener(this)
        popUpFragment!!.show(childFragmentManager,AddTodoFragment.TAG)
    }

    override fun onCheckChange(isChecked: Boolean, taskTextView: TextView, todoData: String) {
        if(isChecked){
            taskTextView.paintFlags = taskTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }else{
            taskTextView.paintFlags = taskTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        firestore.collection("todos")
            .document(todoData)
            .update("checked", isChecked)
            .addOnSuccessListener {
                Toast.makeText(context, "Task updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error updating task: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.d("task","\"Error updating task: ${exception.message}\"")
            }
    }

    override fun logout() {
        logoutPopup.dismiss()
        auth.signOut()
        navControl.navigate(R.id.action_homeFragment_to_signInFragment)
    }
}
