(function () {
    function openModal(id) {
        const modal = document.getElementById(id);
        if (modal) {
            modal.classList.add("open");
            modal.setAttribute("aria-hidden", "false");
        }
    }

    function closeModal(modal) {
        if (modal) {
            modal.classList.remove("open");
            modal.setAttribute("aria-hidden", "true");
        }
    }

    function bindModals() {
        document.querySelectorAll("[data-open-modal]").forEach((button) => {
            button.addEventListener("click", () => openModal(button.dataset.openModal));
        });
        document.querySelectorAll("[data-close-modal]").forEach((button) => {
            button.addEventListener("click", () => closeModal(button.closest(".modal")));
        });
        document.querySelectorAll(".modal").forEach((modal) => {
            modal.addEventListener("click", (event) => {
                if (event.target === modal) {
                    closeModal(modal);
                }
            });
        });
    }

    function bindImportPreview() {
        const form = document.querySelector("[data-import-preview]");
        const confirmButton = document.querySelector("[data-import-confirm]");
        const status = document.getElementById("previewStatus");
        const table = document.getElementById("previewTable");
        if (!form || !confirmButton || !status || !table) {
            return;
        }

        let activeToken = null;
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            confirmButton.disabled = true;
            status.textContent = "正在上传并解析文件...";
            table.innerHTML = "";
            try {
                const response = await fetch("/aggregation/import/preview", {
                    method: "POST",
                    body: new FormData(form)
                });
                if (!response.ok) {
                    throw new Error("上传预览失败");
                }
                const preview = await response.json();
                activeToken = preview.token;
                status.textContent = "已解析 " + preview.table.totalRows + " 行，预览前 " + preview.table.previewRows.length + " 行。";
                table.innerHTML = renderPreviewTable(preview.table.headers, preview.table.previewRows);
                confirmButton.disabled = false;
            } catch (error) {
                status.textContent = error.message;
            }
        });

        confirmButton.addEventListener("click", async () => {
            if (!activeToken) {
                return;
            }
            confirmButton.disabled = true;
            status.textContent = "正在确认入库...";
            const body = new URLSearchParams();
            body.set("token", activeToken);
            const response = await fetch("/aggregation/import/confirm", {
                method: "POST",
                headers: {"Content-Type": "application/x-www-form-urlencoded"},
                body
            });
            const result = await response.json();
            status.textContent = result.message;
            if (result.success) {
                setTimeout(() => window.location.reload(), 900);
            } else {
                confirmButton.disabled = false;
            }
        });
    }

    function renderPreviewTable(headers, rows) {
        if (!headers || headers.length === 0) {
            return "<p class=\"hint\">没有可预览的数据。</p>";
        }
        const head = headers.map((header) => "<th>" + escapeHtml(header) + "</th>").join("");
        const body = rows.map((row) => {
            const cells = headers.map((header) => "<td>" + escapeHtml(row[header] == null ? "" : String(row[header])) + "</td>").join("");
            return "<tr>" + cells + "</tr>";
        }).join("");
        return "<table><thead><tr>" + head + "</tr></thead><tbody>" + body + "</tbody></table>";
    }

    function escapeHtml(value) {
        return value
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;");
    }

    function renderCharts() {
        if (!window.Chart || !window.platformCharts) {
            return;
        }
        const charts = window.platformCharts;
        const palette = ["#f47b20", "#2563eb", "#16a34a", "#dc2626", "#7c3aed", "#0891b2"];
        const line = document.getElementById("lineChart");
        const source = document.getElementById("sourceChart");
        const business = document.getElementById("businessChart");
        if (line) {
            new Chart(line, {
                type: "line",
                data: {
                    labels: charts.lineLabels,
                    datasets: [{
                        label: "记录数",
                        data: charts.lineRows,
                        borderColor: "#f47b20",
                        backgroundColor: "rgba(244,123,32,0.14)",
                        tension: 0.35,
                        fill: true
                    }]
                },
                options: {responsive: true, maintainAspectRatio: false}
            });
        }
        if (source) {
            new Chart(source, {
                type: "doughnut",
                data: {labels: charts.sourceLabels, datasets: [{data: charts.sourceValues, backgroundColor: palette}]},
                options: {responsive: true, maintainAspectRatio: false}
            });
        }
        if (business) {
            new Chart(business, {
                type: "doughnut",
                data: {labels: charts.businessLabels, datasets: [{data: charts.businessValues, backgroundColor: palette.slice().reverse()}]},
                options: {responsive: true, maintainAspectRatio: false}
            });
        }
    }

    document.addEventListener("DOMContentLoaded", () => {
        bindModals();
        bindImportPreview();
        renderCharts();
    });
})();
