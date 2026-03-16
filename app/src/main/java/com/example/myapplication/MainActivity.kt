package com.example.myapplication

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: SetorAdapter
    private lateinit var database: AppDatabase

    private val categorias = arrayOf("Geral", "Servidores", "Câmeras", "Roteadores", "Impressoras", "IOT", "Computador", "Smartphone")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportData(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importData(it) }
    }

    private val pdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { generatePdfReport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        database = AppDatabase.getDatabase(this)
        val rvSetores = findViewById<RecyclerView>(R.id.rvSetores)
        val fabAdd = findViewById<ExtendedFloatingActionButton>(R.id.fabScan)

        adapter = SetorAdapter(
            onItemClick = { setor -> showLogsBottomSheet(setor) },
            onItemLongClick = { setor -> showOptionsDialog(setor) }
        )
        rvSetores.adapter = adapter

        lifecycleScope.launch {
            database.setorDao().getAllSetores().collectLatest { lista ->
                adapter.submitList(lista)
            }
        }

        fabAdd.setOnClickListener {
            showAddSetorDialog()
        }

        rvSetores.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) fabAdd.shrink() else fabAdd.extend()
            }
        })

        setupWorkManager()
        askNotificationPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logs -> {
                showFullLogsDialog()
                return true
            }
            R.id.action_search_network -> {
                showNetworkScanDialog()
                return true
            }
            R.id.action_backup -> {
                exportLauncher.launch("vigilante_backup.json")
                return true
            }
            R.id.action_import -> {
                importLauncher.launch(arrayOf("application/json"))
                return true
            }
            R.id.action_export_report -> {
                pdfLauncher.launch("Relatorio_Rede_${System.currentTimeMillis()}.pdf")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun generatePdfReport(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            val paint = Paint()
            val titlePaint = Paint().apply { textSize = 24f; isFakeBoldText = true }
            val textPaint = Paint().apply { textSize = 14f }
            val headerPaint = Paint().apply { textSize = 16f; isFakeBoldText = true }

            val setores = database.setorDao().getAllSetoresSync()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            setores.forEachIndexed { index, setor ->
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 1).create() // A4 size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                var yPos = 40f

                // Cabeçalho
                canvas.drawText("Relatório de Dispositivo: ${setor.nome}", 40f, yPos, titlePaint)
                yPos += 30f
                canvas.drawText("IP: ${setor.ip} | Categoria: ${setor.categoria} | Uptime: ${"%.2f".format(setor.uptimePercent)}%", 40f, yPos, textPaint)
                yPos += 40f

                // Gráfico de Latência (Desenho Manual Simplificado no PDF)
                canvas.drawText("Gráfico de Latência (Últimos Eventos):", 40f, yPos, headerPaint)
                yPos += 20f
                
                val logs = database.setorDao().getAllLogsForSetorSync(setor.id).take(30).reversed()
                if (logs.isNotEmpty()) {
                    val graphHeight = 100f
                    val graphWidth = 515f
                    val xStep = graphWidth / (logs.size.coerceAtLeast(1))
                    val maxLat = logs.maxOf { it.latencia }.coerceAtLeast(10L).toFloat()

                    paint.color = Color.LTGRAY; paint.strokeWidth = 1f
                    canvas.drawLine(40f, yPos + graphHeight, 40f + graphWidth, yPos + graphHeight, paint) // Eixo X

                    val pathPaint = Paint().apply { color = Color.BLUE; style = Paint.Style.STROKE; strokeWidth = 2f }
                    var lastX = 40f
                    var lastY = yPos + graphHeight
                    
                    logs.forEachIndexed { i, log ->
                        val x = 40f + (i * xStep)
                        val latVal = if (log.latencia > 0) log.latencia.toFloat() else 0f
                        val y = yPos + graphHeight - (latVal / maxLat * graphHeight)
                        if (i > 0) canvas.drawLine(lastX, lastY, x, y, pathPaint)
                        lastX = x; lastY = y
                    }
                    yPos += graphHeight + 30f
                }

                // Tabela de Logs
                canvas.drawText("Últimos Registros:", 40f, yPos, headerPaint)
                yPos += 20f
                
                logs.take(15).forEach { log ->
                    if (yPos > 800f) return@forEach // Evitar overflow simples
                    val status = if (log.isOnline) "ONLINE (${log.latencia}ms)" else "OFFLINE"
                    canvas.drawText("${sdf.format(Date(log.timestamp))} -> $status", 50f, yPos, textPaint)
                    yPos += 20f
                }

                pdfDocument.finishPage(page)
            }

            try {
                contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Relatório PDF gerado com sucesso!", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Erro ao gerar PDF", Toast.LENGTH_SHORT).show() }
            } finally {
                pdfDocument.close()
            }
        }
    }

    private fun exportData(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val setores = database.setorDao().getAllSetoresSync()
                val json = Gson().toJson(setores)
                contentResolver.openOutputStream(uri)?.use { 
                    it.write(json.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, getString(R.string.msg_backup_success), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, getString(R.string.msg_error_export), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun importData(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                val type = object : TypeToken<List<Setor>>() {}.type
                val setoresImportados: List<Setor> = Gson().fromJson(json, type)
                
                val setoresAtuais = database.setorDao().getAllSetoresSync()
                val ipsExistentes = setoresAtuais.map { it.ip }.toSet()

                var novosAdicionados = 0
                setoresImportados.forEach { setor ->
                    if (setor.ip !in ipsExistentes) {
                        database.setorDao().insert(setor.copy(id = 0))
                        novosAdicionados++
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (novosAdicionados > 0) {
                        Toast.makeText(this@MainActivity, getString(R.string.msg_import_success, novosAdicionados), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.msg_no_new_ips), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, getString(R.string.msg_error_import), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showNetworkScanDialog() {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle(R.string.title_scanning)
            .setMessage(getString(R.string.msg_scanning_progress, 0))
            .setCancelable(false)
            .create()
        
        progressDialog.show()

        val prefix = NetworkUtils.getLocalSubnet(this)
        
        NetworkUtils.scanSubnet(prefix, onProgress = { current ->
            runOnUiThread {
                progressDialog.setMessage(getString(R.string.msg_scanning_progress, current))
            }
        }, onComplete = { foundIps ->
            runOnUiThread {
                progressDialog.dismiss()
                if (foundIps.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Nenhum dispositivo encontrado em $prefix.x", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.title_devices_found)
                    .setItems(foundIps.toTypedArray()) { _, which ->
                        showAddSetorDialogWithIp(foundIps[which])
                    }
                    .setNegativeButton(R.string.btn_close, null)
                    .show()
            }
        })
    }

    private fun showAddSetorDialogWithIp(ip: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_setor, null)
        val etNome = dialogView.findViewById<EditText>(R.id.etNome)
        val etIp = dialogView.findViewById<EditText>(R.id.etIp)
        val actvCategoria = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCategoria)
        
        etIp.setText(ip)
        actvCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias))

        AlertDialog.Builder(this)
            .setTitle(R.string.title_new_device)
            .setView(dialogView)
            .setPositiveButton(R.string.action_add) { _, _ ->
                val nome = etNome.text.toString()
                if (nome.isNotBlank()) {
                    lifecycleScope.launch {
                        val id = database.setorDao().insert(Setor(nome = nome, ip = ip, categoria = actvCategoria.text.toString()))
                        database.setorDao().insertLog(LogEvento(setorId = id.toInt(), timestamp = System.currentTimeMillis(), isOnline = true, latencia = 0L))
                        verificarStatusManual(Setor(id = id.toInt(), nome = nome, ip = ip))
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showOptionsDialog(setor: Setor) {
        val options = mutableListOf("Verificar agora", "Editar", "Escanear Portas", "Excluir")
        if (!setor.macAddress.isNullOrBlank()) options.add(2, "Ligar (WOL)")

        AlertDialog.Builder(this)
            .setTitle(setor.nome)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Verificar agora" -> verificarStatusManual(setor)
                    "Editar" -> showEditSetorDialog(setor)
                    "Escanear Portas" -> startPortScan(setor)
                    "Ligar (WOL)" -> wakeOnLan(setor)
                    "Excluir" -> confirmDeleteSetor(setor)
                }
            }
            .show()
    }

    private fun startPortScan(setor: Setor) {
        Toast.makeText(this, "Escaneando portas em ${setor.ip}...", Toast.LENGTH_LONG).show()
        lifecycleScope.launch(Dispatchers.IO) {
            NetworkUtils.scanPorts(setor.ip) { openPorts ->
                lifecycleScope.launch(Dispatchers.Main) {
                    val message = if (openPorts.isEmpty()) "Nenhuma porta aberta." else "Abertas: ${openPorts.joinToString(", ")}"
                    AlertDialog.Builder(this@MainActivity).setTitle("Portas: ${setor.nome}").setMessage(message).setPositiveButton("OK", null).show()
                }
            }
        }
    }

    private fun showFullLogsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.layout_full_logs, null)
        
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_NoTitleBar_Fullscreen)
            .setView(dialogView)
            .create()

        dialog.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            // Tornando a barra de navegação transparente para o fundo do layout aparecer
            window.navigationBarColor = Color.TRANSPARENT
            window.statusBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            }
        }

        val toolbar = dialogView.findViewById<MaterialToolbar>(R.id.toolbarLogs)
        val etSearch = dialogView.findViewById<EditText>(R.id.etSearchLog)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chipGroupCategorias)
        val rvFullLogs = dialogView.findViewById<RecyclerView>(R.id.rvFullLogs)

        toolbar.setNavigationOnClickListener { dialog.dismiss() }

        val logAdapter = FullLogAdapter()
        rvFullLogs.layoutManager = LinearLayoutManager(this); rvFullLogs.adapter = logAdapter

        // IMPORTANTE: Ajustar os insets para a View do Histórico Fullscreen não ficar atrás da barra de navegação
        ViewCompat.setOnApplyWindowInsetsListener(dialogView.findViewById(R.id.rootLogs)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val allChip = Chip(this).apply { text = getString(R.string.cat_all); isCheckable = true; isChecked = true }
        chipGroup.addView(allChip)
        categorias.forEach { cat -> chipGroup.addView(Chip(this).apply { text = cat; isCheckable = true }) }

        var fullList = listOf<LogWithSetor>()
        var filterText = ""
        var filterCat = "Todas"

        fun applyFilters() {
            val filtered = fullList.filter { item ->
                (filterCat == "Todas" || item.setor.categoria == filterCat) &&
                (item.setor.nome.contains(filterText, ignoreCase = true) || item.setor.ip.contains(filterText))
            }
            logAdapter.submitList(filtered)
        }

        lifecycleScope.launch {
            database.setorDao().getAllLogsWithSetor().collectLatest { logs ->
                fullList = logs; applyFilters()
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterText = s.toString(); applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            filterCat = if (checkedId != null) group.findViewById<Chip>(checkedId).text.toString() else "Todas"
            applyFilters()
        }

        dialog.show()
    }

    private fun showLogsBottomSheet(setor: Setor) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_logs_bottom_sheet, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvSheetTitle)
        val rvLogs = view.findViewById<RecyclerView>(R.id.rvLogs)
        val btnVerificar = view.findViewById<TextView>(R.id.btnVerificarAgora)
        val latencyChart = view.findViewById<LatencyChartView>(R.id.latencyChart)

        tvTitle.text = getString(R.string.log_last_verification, setor.nome)
        val logAdapter = LogAdapter()
        rvLogs.layoutManager = LinearLayoutManager(this); rvLogs.adapter = logAdapter

        lifecycleScope.launch {
            database.setorDao().getLogsForSetor(setor.id).collectLatest { logs -> 
                logAdapter.submitList(logs)
                // Atualiza o gráfico sempre que novos logs chegarem via Flow
                latencyChart.setData(logs.map { it.latencia })
            }
        }

        btnVerificar.setOnClickListener { verificarStatusManual(setor) }
        dialog.setContentView(view); dialog.show()
    }

    private fun wakeOnLan(setor: Setor) {
        setor.macAddress?.let { mac ->
            lifecycleScope.launch(Dispatchers.IO) {
                NetworkUtils.sendWakeOnLan(mac)
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "WOL enviado", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun showEditSetorDialog(setor: Setor) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_setor, null)
        val etNome = dialogView.findViewById<EditText>(R.id.etNome)
        val etIp = dialogView.findViewById<EditText>(R.id.etIp)
        val actvCategoria = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCategoria)
        val etMac = dialogView.findViewById<EditText>(R.id.etMac)

        etNome.setText(setor.nome); etIp.setText(setor.ip); etMac.setText(setor.macAddress)
        actvCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias))
        actvCategoria.setText(setor.categoria, false)

        AlertDialog.Builder(this).setTitle(R.string.title_edit_device).setView(dialogView).setPositiveButton(R.string.action_save) { _, _ ->
            val novoNome = etNome.text.toString(); val novoIp = etIp.text.toString()
            if (novoNome.isNotBlank() && novoIp.isNotBlank()) {
                lifecycleScope.launch { database.setorDao().update(setor.copy(nome = novoNome, ip = novoIp, categoria = actvCategoria.text.toString(), macAddress = etMac.text.toString())) }
            }
        }.setNegativeButton(R.string.action_cancel, null).show()
    }

    private fun confirmDeleteSetor(setor: Setor) {
        AlertDialog.Builder(this).setTitle(R.string.action_delete).setMessage(getString(R.string.msg_confirm_delete, setor.nome)).setPositiveButton(R.string.action_delete) { _, _ ->
            lifecycleScope.launch { database.setorDao().delete(setor) }
        }.setNegativeButton(R.string.action_cancel, null).show()
    }

    private fun verificarStatusManual(setor: Setor) {
        Toast.makeText(this, getString(R.string.msg_verifying), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { NetworkUtils.checkStatusWithLatency(setor.ip) }
            database.setorDao().insertLog(LogEvento(setorId = setor.id, timestamp = System.currentTimeMillis(), isOnline = result.isOnline, latencia = result.latency))
            database.setorDao().update(setor.copy(isOnline = result.isOnline, latencia = result.latency, ultimaVerificacao = System.currentTimeMillis()))
            val status = if (result.isOnline) "Online (${result.latency}ms)" else "Offline"
            Toast.makeText(this@MainActivity, "${setor.nome}: $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddSetorDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_setor, null)
        val etNome = dialogView.findViewById<EditText>(R.id.etNome)
        val etIp = dialogView.findViewById<EditText>(R.id.etIp)
        val actvCategoria = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCategoria)
        val etMac = dialogView.findViewById<EditText>(R.id.etMac)

        actvCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias))

        AlertDialog.Builder(this).setTitle(R.string.title_new_device).setView(dialogView).setPositiveButton(R.string.action_add) { _, _ ->
            val nome = etNome.text.toString(); val ip = etIp.text.toString()
            if (nome.isNotBlank() && ip.isNotBlank()) {
                lifecycleScope.launch {
                    val id = database.setorDao().insert(Setor(nome = nome, ip = ip, categoria = actvCategoria.text.toString(), macAddress = etMac.text.toString()))
                    database.setorDao().insertLog(LogEvento(setorId = id.toInt(), timestamp = System.currentTimeMillis(), isOnline = false, latencia = -1L))
                    verificarStatusManual(Setor(id = id.toInt(), nome = nome, ip = ip))
                }
            }
        }.setNegativeButton(R.string.action_cancel, null).show()
    }

    private fun setupWorkManager() {
        val workRequest = PeriodicWorkRequestBuilder<MonitoramentoWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("MonitoramentoRede", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
