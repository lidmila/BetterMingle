package com.bettermingle.app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.bettermingle.app.data.model.DetailedEventReport
import com.bettermingle.app.ui.screen.event.SummaryStats
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfSection { PARTICIPANTS, POLLS, BUDGET, EXPENSES, WISHLIST, TASKS, PACKING, CARPOOL }

class EventPdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN_LEFT = 40f
        private const val MARGIN_RIGHT = 40f
        private const val MARGIN_TOP = 50f
        private const val MARGIN_BOTTOM = 60f
        private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

        private const val ACCENT_COLOR = 0xFF5B5FEF.toInt()
        private const val TEXT_COLOR = 0xFF1A1B3D.toInt()
        private const val SECONDARY_COLOR = 0xFF6E7191.toInt()
        private const val TABLE_HEADER_BG = 0xFFDCD9F5.toInt()
        private const val TABLE_LINE_COLOR = 0xFFE0DFF0.toInt()
        private const val ZEBRA_ROW_BG = 0xFFF1EFF6.toInt()
        private const val HEADER_BG = 0xFF5B5FEF.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val ACCENT_PINK = 0xFFE879B8.toInt()
    }

    private lateinit var document: PdfDocument
    private lateinit var currentPage: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var pageNumber = 0
    private var currentY = MARGIN_TOP

    private val titlePaint = TextPaint().apply {
        isAntiAlias = true
        color = TEXT_COLOR
        textSize = 24f
        isFakeBoldText = true
    }

    private val headerTitlePaint = TextPaint().apply {
        isAntiAlias = true
        color = WHITE
        textSize = 22f
        isFakeBoldText = true
    }

    private val sectionPaint = TextPaint().apply {
        isAntiAlias = true
        color = ACCENT_COLOR
        textSize = 16f
        isFakeBoldText = true
    }

    private val subsectionPaint = TextPaint().apply {
        isAntiAlias = true
        color = TEXT_COLOR
        textSize = 13f
        isFakeBoldText = true
    }

    private val labelPaint = TextPaint().apply {
        isAntiAlias = true
        color = SECONDARY_COLOR
        textSize = 12f
    }

    private val valuePaint = TextPaint().apply {
        isAntiAlias = true
        color = TEXT_COLOR
        textSize = 12f
    }

    private val tableBoldPaint = TextPaint().apply {
        isAntiAlias = true
        color = TEXT_COLOR
        textSize = 11f
        isFakeBoldText = true
    }

    private val tablePaint = TextPaint().apply {
        isAntiAlias = true
        color = TEXT_COLOR
        textSize = 11f
    }

    private val tableSecondaryPaint = TextPaint().apply {
        isAntiAlias = true
        color = SECONDARY_COLOR
        textSize = 10f
    }

    private val footerPaint = TextPaint().apply {
        isAntiAlias = true
        color = SECONDARY_COLOR
        textSize = 10f
    }

    private val footerBoldPaint = TextPaint().apply {
        isAntiAlias = true
        color = TEXT_COLOR
        textSize = 10f
        isFakeBoldText = true
    }

    private val accentLinePaint = Paint().apply {
        isAntiAlias = true
        color = ACCENT_COLOR
        strokeWidth = 2f
    }

    private val tableLinePaint = Paint().apply {
        isAntiAlias = true
        color = TABLE_LINE_COLOR
        strokeWidth = 0.5f
    }

    private val tableHeaderBgPaint = Paint().apply {
        isAntiAlias = true
        color = TABLE_HEADER_BG
        style = Paint.Style.FILL
    }

    private val headerBgPaint = Paint().apply {
        isAntiAlias = true
        color = HEADER_BG
        style = Paint.Style.FILL
    }

    private val pinkLinePaint = Paint().apply {
        isAntiAlias = true
        color = ACCENT_PINK
        strokeWidth = 3f
    }

    private val zebraRowPaint = Paint().apply {
        isAntiAlias = true
        color = ZEBRA_ROW_BG
        style = Paint.Style.FILL
    }

    private val bulletPaint = Paint().apply {
        isAntiAlias = true
        color = ACCENT_COLOR
        style = Paint.Style.FILL
    }

    private val footerLinePaint = Paint().apply {
        isAntiAlias = true
        color = ACCENT_COLOR
        strokeWidth = 0.5f
    }

    private val dividerPaint = Paint().apply {
        isAntiAlias = true
        color = TABLE_LINE_COLOR
        strokeWidth = 0.5f
    }

    // ──────────────────────────────────────────────
    // Original summary PDF (kept for backward compat)
    // ──────────────────────────────────────────────

    internal fun generate(stats: SummaryStats): File {
        val reportsDir = File(context.cacheDir, "reports")
        reportsDir.listFiles()?.forEach { it.delete() }
        reportsDir.mkdirs()

        document = PdfDocument()
        startNewPage()

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        drawWrappedText(stats.eventName, titlePaint)
        currentY += 6f

        if (stats.startDate != null) {
            val dateStr = buildString {
                append(dateFormat.format(Date(stats.startDate)))
                if (stats.endDate != null) {
                    append(" – ")
                    append(dateFormat.format(Date(stats.endDate)))
                }
            }
            drawWrappedText(dateStr, labelPaint)
        }
        if (stats.locationName.isNotEmpty()) drawWrappedText(stats.locationName, labelPaint)
        if (stats.eventTheme.isNotEmpty()) drawKeyValue("Theme", stats.eventTheme)
        if (stats.status.isNotEmpty()) drawKeyValue("Status", stats.status)
        currentY += 16f

        drawSectionTitle("Participants")
        drawKeyValue("Total", "${stats.participantCount}")
        drawKeyValue("Accepted", "${stats.acceptedCount}")
        drawKeyValue("Declined", "${stats.declinedCount}")
        drawKeyValue("Maybe", "${stats.maybeCount}")
        drawKeyValue("Pending", "${stats.pendingCount}")
        currentY += 16f

        drawSectionTitle("Expenses")
        drawKeyValue("Total", "${String.format("%,.0f", stats.totalExpenses)} CZK")
        if (stats.topPayer.isNotEmpty()) {
            drawKeyValue("Top payer", "${stats.topPayer} (${String.format("%,.0f", stats.topPayerAmount)} CZK)")
        }
        currentY += 16f

        drawSectionTitle("Polls")
        drawKeyValue("Total", "${stats.pollCount}")
        drawKeyValue("Active", "${stats.activePollCount}")
        drawKeyValue("Closed", "${stats.closedPollCount}")
        currentY += 16f

        drawSectionTitle("Activity")
        drawKeyValue("Messages", "${stats.messageCount}")
        if (stats.mostActiveParticipant.isNotEmpty()) drawKeyValue("Most active", stats.mostActiveParticipant)
        currentY += 16f

        drawSectionTitle("Other modules")
        drawKeyValue("Rides", "${stats.rideCount}")
        drawKeyValue("Tasks", "${stats.taskCount}")
        drawKeyValue("Packing items", "${stats.packingItemCount}")
        drawKeyValue("Wishlist items", "${stats.wishlistItemCount}")

        drawFooter()
        document.finishPage(currentPage)

        val file = File(reportsDir, "event_report.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        return file
    }

    // ──────────────────────────────────────────────
    // Detailed PDF export
    // ──────────────────────────────────────────────

    internal fun generateDetailed(
        report: DetailedEventReport,
        enabledSections: Set<PdfSection> = PdfSection.entries.toSet()
    ): File {
        val reportsDir = File(context.cacheDir, "reports")
        reportsDir.listFiles()?.forEach { it.delete() }
        reportsDir.mkdirs()

        document = PdfDocument()
        pageNumber = 0
        currentY = MARGIN_TOP
        startNewPage()

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        // ── 1. Branded Header Band ──
        val bandHeight = 70f
        val bandRect = RectF(MARGIN_LEFT - 10f, currentY - 10f, PAGE_WIDTH - MARGIN_RIGHT + 10f, currentY + bandHeight)
        canvas.drawRoundRect(bandRect, 8f, 8f, headerBgPaint)
        canvas.drawText(report.eventName, MARGIN_LEFT + 6f, currentY + 30f, headerTitlePaint)

        // Date inside band
        if (report.startDate != null) {
            val dateStr = buildString {
                append(dateFormat.format(Date(report.startDate)))
                if (report.endDate != null) {
                    append(" – ")
                    append(dateFormat.format(Date(report.endDate)))
                }
            }
            val whiteLabelPaint = TextPaint().apply {
                isAntiAlias = true
                color = WHITE
                textSize = 11f
            }
            canvas.drawText(dateStr, MARGIN_LEFT + 6f, currentY + 50f, whiteLabelPaint)
        }

        currentY += bandHeight + 3f
        // Decorative pink line below band
        canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, pinkLinePaint)
        currentY += 12f

        // ── Info below header ──
        if (report.locationName.isNotEmpty()) drawWrappedText(report.locationName, labelPaint)
        if (report.eventTheme.isNotEmpty()) drawKeyValue("Theme", report.eventTheme)
        if (report.status.isNotEmpty()) drawKeyValue("Status", report.status)
        if (report.inviteCode.isNotEmpty()) drawKeyValue("Invite code", report.inviteCode)
        if (report.eventDescription.isNotEmpty()) {
            currentY += 4f
            drawWrappedText(report.eventDescription, labelPaint)
        }
        drawSectionDivider()

        // ── 2. Participants ──
        if (PdfSection.PARTICIPANTS in enabledSections && report.participants.isNotEmpty()) {
            drawSectionTitle("Participants")
            val colWidths = floatArrayOf(CONTENT_WIDTH * 0.65f, CONTENT_WIDTH * 0.35f)
            drawTableHeader(arrayOf("Name", "RSVP"), colWidths)
            report.participants.forEachIndexed { index, p ->
                drawTableRow(arrayOf(p.displayName, p.rsvp), colWidths, tablePaint, index)
            }
            val accepted = report.participants.count { it.rsvp == "ACCEPTED" }
            val declined = report.participants.count { it.rsvp == "DECLINED" }
            val maybe = report.participants.count { it.rsvp == "MAYBE" }
            val pending = report.participants.count { it.rsvp != "ACCEPTED" && it.rsvp != "DECLINED" && it.rsvp != "MAYBE" }
            currentY += 4f
            drawWrappedText(
                "Total: ${report.participants.size} ($accepted accepted, $declined declined, $maybe maybe, $pending pending)",
                tableSecondaryPaint
            )
            drawSectionDivider()
        }

        // ── 3. Polls ──
        if (PdfSection.POLLS in enabledSections && report.polls.isNotEmpty()) {
            drawSectionTitle("Polls")
            for (poll in report.polls) {
                val statusStr = if (poll.isClosed) "[Closed]" else "[Active]"
                drawSubsectionTitle("${poll.title} $statusStr")
                if (poll.options.isNotEmpty()) {
                    val colWidths = floatArrayOf(CONTENT_WIDTH * 0.7f, CONTENT_WIDTH * 0.3f)
                    drawTableHeader(arrayOf("Option", "Votes"), colWidths)
                    poll.options.forEachIndexed { index, opt ->
                        drawTableRow(arrayOf(opt.label, "${opt.voteCount}"), colWidths, tablePaint, index)
                    }
                }
                currentY += 8f
            }
            drawSectionDivider()
        }

        // ── 4. Budget ──
        if (PdfSection.BUDGET in enabledSections && report.budgetCategories.isNotEmpty()) {
            drawSectionTitle("Budget")
            val colWidths = floatArrayOf(CONTENT_WIDTH * 0.4f, CONTENT_WIDTH * 0.3f, CONTENT_WIDTH * 0.3f)
            drawTableHeader(arrayOf("Category", "Planned", "Actual"), colWidths)
            var totalPlanned = 0.0
            var totalActual = 0.0
            report.budgetCategories.forEachIndexed { index, cat ->
                drawTableRow(
                    arrayOf(cat.name, String.format("%,.0f", cat.planned), String.format("%,.0f", cat.actualTotal)),
                    colWidths, tablePaint, index
                )
                totalPlanned += cat.planned
                totalActual += cat.actualTotal
            }
            drawTableRow(
                arrayOf("Total", String.format("%,.0f", totalPlanned), String.format("%,.0f", totalActual)),
                colWidths, tableBoldPaint, -1
            )
            drawSectionDivider()
        }

        // ── 5. Expenses ──
        if (PdfSection.EXPENSES in enabledSections && report.expenses.isNotEmpty()) {
            drawSectionTitle("Expenses")
            val colWidths = floatArrayOf(CONTENT_WIDTH * 0.4f, CONTENT_WIDTH * 0.3f, CONTENT_WIDTH * 0.3f)
            drawTableHeader(arrayOf("Description", "Paid by", "Amount"), colWidths)
            var grandTotal = 0.0
            var rowIdx = 0
            for (exp in report.expenses) {
                drawTableRow(
                    arrayOf(exp.description, exp.paidByName, "${String.format("%,.0f", exp.amount)} ${exp.currency}"),
                    colWidths, tablePaint, rowIdx++
                )
                grandTotal += exp.amount
                for (split in exp.splits) {
                    val settled = if (split.isSettled) " [settled]" else ""
                    drawTableRow(
                        arrayOf("  → ${split.userName}", "", "${String.format("%,.0f", split.amount)}$settled"),
                        colWidths, tableSecondaryPaint, -1
                    )
                }
            }
            currentY += 2f
            drawTableRow(
                arrayOf("Grand total", "", "${String.format("%,.0f", grandTotal)} CZK"),
                colWidths, tableBoldPaint, -1
            )
            drawSectionDivider()
        }

        // ── 6. Wishlist ──
        if (PdfSection.WISHLIST in enabledSections && report.wishlistItems.isNotEmpty()) {
            drawSectionTitle("Wishlist")
            val colWidths = floatArrayOf(CONTENT_WIDTH * 0.3f, CONTENT_WIDTH * 0.2f, CONTENT_WIDTH * 0.25f, CONTENT_WIDTH * 0.25f)
            drawTableHeader(arrayOf("Item", "Price", "Status", "Claimed by"), colWidths)
            report.wishlistItems.forEachIndexed { index, item ->
                drawTableRow(
                    arrayOf(
                        item.name,
                        item.price?.let { String.format("%,.0f", it) } ?: "-",
                        item.status,
                        item.claimedByName ?: "-"
                    ),
                    colWidths, tablePaint, index
                )
            }
            drawSectionDivider()
        }

        // ── 7. Tasks ──
        if (PdfSection.TASKS in enabledSections && report.tasks.isNotEmpty()) {
            drawSectionTitle("Tasks")
            val colWidths = floatArrayOf(CONTENT_WIDTH * 0.4f, CONTENT_WIDTH * 0.4f, CONTENT_WIDTH * 0.2f)
            drawTableHeader(arrayOf("Task", "Assigned to", "Done?"), colWidths)
            report.tasks.forEachIndexed { index, task ->
                drawTableRow(
                    arrayOf(
                        task.name,
                        task.assignedToNames.joinToString(", ").ifEmpty { "-" },
                        if (task.isCompleted) "Yes" else "No"
                    ),
                    colWidths, tablePaint, index
                )
            }
            drawSectionDivider()
        }

        // ── 8. Packing List ──
        if (PdfSection.PACKING in enabledSections && report.packingItems.isNotEmpty()) {
            drawSectionTitle("Packing List")
            val colWidths = floatArrayOf(CONTENT_WIDTH * 0.4f, CONTENT_WIDTH * 0.35f, CONTENT_WIDTH * 0.25f)
            drawTableHeader(arrayOf("Item", "Responsible", "Packed?"), colWidths)
            report.packingItems.forEachIndexed { index, item ->
                drawTableRow(
                    arrayOf(
                        item.name,
                        item.responsibleName ?: "-",
                        if (item.isChecked) "Yes" else "No"
                    ),
                    colWidths, tablePaint, index
                )
            }
            drawSectionDivider()
        }

        // ── 9. Carpool ──
        if (PdfSection.CARPOOL in enabledSections && report.carpoolRides.isNotEmpty()) {
            drawSectionTitle("Carpool")
            for (ride in report.carpoolRides) {
                val timeStr = ride.departureTime?.let { dateTimeFormat.format(Date(it)) } ?: ""
                drawSubsectionTitle("Driver: ${ride.driverName}")
                if (ride.departureLocation.isNotEmpty()) drawKeyValue("From", ride.departureLocation)
                if (timeStr.isNotEmpty()) drawKeyValue("Departure", timeStr)
                drawKeyValue("Seats", "${ride.availableSeats}")
                if (ride.type.isNotEmpty()) drawKeyValue("Type", ride.type)
                if (ride.passengers.isNotEmpty()) {
                    val colWidths = floatArrayOf(CONTENT_WIDTH * 0.6f, CONTENT_WIDTH * 0.4f)
                    drawTableHeader(arrayOf("Passenger", "Status"), colWidths)
                    ride.passengers.forEachIndexed { index, p ->
                        drawTableRow(arrayOf(p.displayName, p.status), colWidths, tablePaint, index)
                    }
                }
                currentY += 8f
            }
            drawSectionDivider()
        }

        // ── Finish ──
        drawFooter()
        document.finishPage(currentPage)

        val safeName = report.eventName
            .replace(Regex("[^a-zA-Z0-9áčďéěíňóřšťúůýžÁČĎÉĚÍŇÓŘŠŤÚŮÝŽ _-]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .ifEmpty { "report" }
        val file = File(reportsDir, "${safeName}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        return file
    }

    // ──────────────────────────────────────────────
    // Drawing helpers
    // ──────────────────────────────────────────────

    private fun startNewPage() {
        if (pageNumber > 0) {
            drawFooter()
            document.finishPage(currentPage)
        }
        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        currentPage = document.startPage(pageInfo)
        canvas = currentPage.canvas
        currentY = MARGIN_TOP
    }

    private fun checkPageBreak(neededHeight: Float = 40f) {
        if (currentY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
            startNewPage()
        }
    }

    private fun drawSectionTitle(title: String) {
        checkPageBreak(40f)
        // Bullet circle before title
        val bulletY = currentY + sectionPaint.textSize - 4f
        canvas.drawCircle(MARGIN_LEFT + 4f, bulletY, 4f, bulletPaint)
        canvas.drawText(title, MARGIN_LEFT + 16f, currentY + sectionPaint.textSize, sectionPaint)
        currentY += sectionPaint.textSize + 4f
        // Underline extends to text width + 16px
        val textWidth = sectionPaint.measureText(title)
        canvas.drawLine(MARGIN_LEFT + 16f, currentY, MARGIN_LEFT + 16f + textWidth + 16f, currentY, accentLinePaint)
        currentY += 10f
    }

    private fun drawSubsectionTitle(title: String) {
        checkPageBreak(28f)
        canvas.drawText(title, MARGIN_LEFT, currentY + subsectionPaint.textSize, subsectionPaint)
        currentY += subsectionPaint.textSize + 6f
    }

    private fun drawKeyValue(label: String, value: String) {
        checkPageBreak(20f)
        canvas.drawText("$label:", MARGIN_LEFT, currentY + labelPaint.textSize, labelPaint)
        canvas.drawText(value, MARGIN_LEFT + 120f, currentY + valuePaint.textSize, valuePaint)
        currentY += valuePaint.textSize + 6f
    }

    private fun drawWrappedText(text: String, paint: TextPaint) {
        val width = CONTENT_WIDTH.toInt()
        @Suppress("DEPRECATION")
        val layout = StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false)
        val height = layout.height.toFloat()
        checkPageBreak(height)
        canvas.save()
        canvas.translate(MARGIN_LEFT, currentY)
        layout.draw(canvas)
        canvas.restore()
        currentY += height + 4f
    }

    private fun drawTableHeader(columns: Array<String>, widths: FloatArray) {
        val rowHeight = tableBoldPaint.textSize + 10f
        checkPageBreak(rowHeight)
        // Rounded background
        val rect = RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + rowHeight)
        canvas.drawRoundRect(rect, 4f, 4f, tableHeaderBgPaint)
        // Text
        var x = MARGIN_LEFT + 4f
        for (i in columns.indices) {
            val text = clipText(columns[i], widths[i] - 8f, tableBoldPaint)
            canvas.drawText(text, x, currentY + tableBoldPaint.textSize + 3f, tableBoldPaint)
            x += widths[i]
        }
        // Underline
        canvas.drawLine(MARGIN_LEFT, currentY + rowHeight, MARGIN_LEFT + CONTENT_WIDTH, currentY + rowHeight, tableLinePaint)
        currentY += rowHeight
    }

    private fun drawTableRow(columns: Array<String>, widths: FloatArray, paint: TextPaint, rowIndex: Int = -1) {
        val rowHeight = paint.textSize + 8f
        checkPageBreak(rowHeight)
        // Zebra striping for even rows
        if (rowIndex >= 0 && rowIndex % 2 == 0) {
            canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + rowHeight, zebraRowPaint)
        }
        var x = MARGIN_LEFT + 4f
        for (i in columns.indices) {
            val text = clipText(columns[i], widths[i] - 8f, paint)
            canvas.drawText(text, x, currentY + paint.textSize + 2f, paint)
            x += widths[i]
        }
        currentY += rowHeight
    }

    private fun drawSectionDivider() {
        currentY += 8f
        checkPageBreak(16f)
        val inset = 40f
        canvas.drawLine(MARGIN_LEFT + inset, currentY, PAGE_WIDTH - MARGIN_RIGHT - inset, currentY, dividerPaint)
        currentY += 16f
    }

    private fun clipText(text: String, maxWidth: Float, paint: TextPaint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return if (end > 0) text.substring(0, end) + "…" else text.take(1)
    }

    private fun drawFooter() {
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val footerY = PAGE_HEIGHT - 45f
        // Thin accent line
        canvas.drawLine(MARGIN_LEFT, footerY, PAGE_WIDTH - MARGIN_RIGHT, footerY, footerLinePaint)
        // Left: "BetterMingle" bold
        canvas.drawText("BetterMingle", MARGIN_LEFT, footerY + 16f, footerBoldPaint)
        // Center: page number
        val pageText = "Page $pageNumber"
        val pageTextWidth = footerPaint.measureText(pageText)
        canvas.drawText(pageText, (PAGE_WIDTH - pageTextWidth) / 2f, footerY + 16f, footerPaint)
        // Right: timestamp
        val tsWidth = footerPaint.measureText(timestamp)
        canvas.drawText(timestamp, PAGE_WIDTH - MARGIN_RIGHT - tsWidth, footerY + 16f, footerPaint)
    }
}
