/*
 * Copyright (C) 2008-2026, Juick
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.juick.android.widget

import android.app.Dialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.canhub.cropper.CropImageView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.juick.R
import com.juick.databinding.DialogCropBinding
import java.io.File
import java.io.FileOutputStream

class CropBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogCropBinding? = null
    private val binding get() = _binding!!
    var onCropResult: ((Uri?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_BottomSheet_Crop)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        object : BottomSheetDialog(requireContext(), theme) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()

                window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                }

                findViewById<View>(com.google.android.material.R.id.container)?.apply {
                    fitsSystemWindows = false
                    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
                        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                        val params = view.layoutParams as ViewGroup.MarginLayoutParams
                        params.topMargin = insets.top
                        view.layoutParams = params
                        WindowInsetsCompat.CONSUMED
                    }
                }

                findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows = false
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCropBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.6f)
        }

        val imageUri = requireArguments().getParcelable<Uri>(ARG_IMAGE_URI)
        if (imageUri != null) {
            binding.cropImageView.setImageUriAsync(imageUri)
        }

        binding.cropConfirm.setOnClickListener { cropAndReturn() }
        binding.cropCancel.setOnClickListener { dismiss() }
    }

    override fun onResume() {
        super.onResume()
        dialog?.let { dialog ->
            val bottomSheet = (dialog as BottomSheetDialog).findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return
            val behavior = BottomSheetBehavior.from(bottomSheet)
            val maxHeight = (resources.displayMetrics.heightPixels * 0.8).toInt()
            bottomSheet.layoutParams.height = maxHeight
            behavior.isFitToContents = true
            behavior.halfExpandedRatio = 0.8f
            behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            behavior.skipCollapsed = true
            behavior.isDraggable = false
        }
    }

    private fun cropAndReturn() {
        binding.cropImageView.setOnCropImageCompleteListener { _: CropImageView, result: CropImageView.CropResult ->
            val uri = if (result.isSuccessful) {
                saveBitmapToFile(result.bitmap)
            } else {
                null
            }
            onCropResult?.invoke(uri)
            dismiss()
        }
        binding.cropImageView.croppedImageAsync()
    }

    private fun saveBitmapToFile(bitmap: Bitmap?): Uri? {
        if (bitmap == null) return null
        val dir = File(requireContext().filesDir, "cropped")
        dir.mkdirs()
        val file = File(dir, "crop_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "crop_bottom_sheet"
        private const val ARG_IMAGE_URI = "image_uri"

        fun newInstance(imageUri: Uri): CropBottomSheet {
            return CropBottomSheet().apply {
                arguments = Bundle().apply { putParcelable(ARG_IMAGE_URI, imageUri) }
            }
        }
    }
}
