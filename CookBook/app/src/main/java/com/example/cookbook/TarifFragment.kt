package com.example.cookbook

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Camera
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.example.cookbook.databinding.FragmentTarifBinding
import java.io.ByteArrayOutputStream
import java.lang.Exception

class TarifFragment : Fragment() {

    var secilenGorsel : Uri? = null
    var secilenBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    private var _binding : FragmentTarifBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_tarif, container, false)

        _binding = FragmentTarifBinding.inflate(inflater, container, false)
        val view = binding.root
        return view

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.kaydet.setOnClickListener {
            kaydet(it)
        }

        binding.imageView.setOnClickListener {
            gorselSec(it)

        }

        arguments?.let {
             var gelenBilgi = TarifFragmentArgs.fromBundle(it).bilgi

            if (gelenBilgi.equals("menudengeldim")){
                //yeni bir yemek eklemeye geldim
                binding.cookNameText.setText("")
                binding.cookMalzemeText.setText("")
                binding.kaydet.visibility = View.VISIBLE

                val gorselsecmearkaplanı = BitmapFactory.decodeResource(context?.resources, R.drawable.tap_here)
                binding.imageView.setImageBitmap(gorselsecmearkaplanı)


            }else {
                // seçilen yemeği görmeye geldim
                binding.kaydet.visibility = View.INVISIBLE

                val secilenId = TarifFragmentArgs.fromBundle(it).id

                context?.let {
                    try {

                        val db = it.openOrCreateDatabase("YEMEKLER", Context.MODE_PRIVATE, null)
                        val cursor = db.rawQuery("SELECT * FROM yemekler WHERE id = ?" , arrayOf(secilenId.toString()))

                        val yemekIsmiIndex = cursor.getColumnIndex("yemekismi")
                        val yemekMalzemeIndex = cursor.getColumnIndex("yemekmalzemesi")
                        val yemekgorseli = cursor.getColumnIndex("gorsel")


                        while (cursor.moveToNext()){
                            binding.cookNameText.setText(cursor.getString(yemekIsmiIndex))
                            binding.cookMalzemeText.setText(cursor.getString(yemekMalzemeIndex))

                            val byteDizisi = cursor.getBlob(yemekgorseli)
                            val bitmap = BitmapFactory.decodeByteArray(byteDizisi, 0, byteDizisi.size)
                            binding.imageView.setImageBitmap(bitmap)

                        }

                        cursor.close()

                    }catch (e : Exception){
                        e.printStackTrace()
                    }
                }

            }
        }

    }

    fun kaydet (view : View){

        val yemekIsmi = binding.cookNameText.text.toString()
        val yemekMalzemeleri = binding.cookMalzemeText.text.toString()

        if (secilenBitmap != null){
            val kucukBitmap = bitmapkucult(secilenBitmap!!, 300)
            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG, 50,outputStream)
            val byteDizisi = outputStream.toByteArray()


            try {

                context?.let {

                    val database = it.openOrCreateDatabase("YEMEKLER", Context.MODE_PRIVATE,null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS yemekler(id INTEGER PRIMARY KEY, yemekismi VARCHAR, yemekmalzemesi VARCHAR, gorsel BLOB)")

                    val sqlString = "INSERT INTO yemekler (yemekismi,yemekmalzemesi,gorsel) VALUES (?,?,?)"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1, yemekIsmi)
                    statement.bindString(2, yemekMalzemeleri)
                    statement.bindBlob(3,byteDizisi)
                    statement.execute()



                }

            }catch (e : Exception){
                e.printStackTrace()
            }


            val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
            Navigation.findNavController(view).navigate(action)

        }

    }

    //granted izin verildi demek


    fun gorselSec (view: View){

        activity?.let {
            if (ContextCompat.checkSelfPermission(it.applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //izin verilmedi izin istememiz lazım
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

            }
            else {
                //izin zaten verilmiş izin istemede galeriye git

                val galeriIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent, 2)
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1){
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                //izni aldık
                val galeriIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent, 2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null){
            secilenGorsel = data?.data

            try {

                context?.let {
                    if (secilenGorsel != null){
                        if (Build.VERSION.SDK_INT >= 28){
                            val source = ImageDecoder.createSource(it.contentResolver, secilenGorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }else {
                            secilenBitmap = MediaStore.Images.Media.getBitmap(it.contentResolver, secilenGorsel)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }
                    }
                }

            }catch (e : Exception){
                e.printStackTrace()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }


    fun bitmapkucult(kullanıcınınSectigiBitmap: Bitmap, maximumBoyut : Int) : Bitmap{

        var width = kullanıcınınSectigiBitmap.width
        var height = kullanıcınınSectigiBitmap.height

        val bitmapOrani : Double = width.toDouble() / height.toDouble()

        if (bitmapOrani > 1){
            //görselimiz yatay

            width = maximumBoyut
            val kısaltılmısHeight = width / bitmapOrani
            height = kısaltılmısHeight.toInt()

        }else {
            //görsel dikey
            height = maximumBoyut
            val kısaltılmısWidth = height* bitmapOrani
            width= kısaltılmısWidth.toInt()
        }

        return Bitmap.createScaledBitmap(kullanıcınınSectigiBitmap,width, height,true)

    }

    /*private fun CameraPermission(){
        Dexter.withContext(this)
            .withPermission(
                android.Manifest.permission.CAMERA).withListener(

                object  : DexterBuilder.SinglePermissionListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                        report?.let{


                            if (report.areAllPermissionsGranted()){
                                Camera
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        TODO("Not yet implemented")
                    }


                }
            )
    }*/


}



















