package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

private const val PREFS_NAME = "prefs"
private const val PREF_UPLOADED_IMAGE_URL = "uploaded_image_url"

@Composable
fun ImagePicker() {
    val context = LocalContext.current

    var selectImageUri by remember { mutableStateOf<Uri?>(null) }
    var upLoadImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        upLoadImageUrl = getImageUrlFromPrefs(context)
    }

    if (isUploading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, trackColor = Color.Red, strokeCap = StrokeCap.Round)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { selectImageUri = it }
        })

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = { launcher.launch("image/*") }) {
            Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "")
        }
    }

    selectImageUri?.let { uri ->
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${uri.lastPathSegment}")

        LaunchedEffect(uri) {
            try {
                isUploading = true
                imageRef.putFile(uri).await()
                val downloadUrl = imageRef.downloadUrl.await()
                upLoadImageUrl = downloadUrl.toString()
                saveImageUrlToPrefs(context, upLoadImageUrl!!)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isUploading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        upLoadImageUrl?.let {
            Spacer(modifier = Modifier.height(16.dp))
            AsyncImage(model = it, contentDescription = "Uploaded Image", modifier = Modifier.size(200.dp))
        }
    }
}

private fun saveImageUrlToPrefs(context: Context, url: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(PREF_UPLOADED_IMAGE_URL, url).apply()
}

private fun getImageUrlFromPrefs(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(PREF_UPLOADED_IMAGE_URL, null)
}
