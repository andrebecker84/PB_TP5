/* ============================================================
   SAIKOO Dashboard — Inicialização dos gráficos Chart.js
   Chamado via dashboard.html com os dados injetados pelo Thymeleaf
   ============================================================ */

(function (ativos, alocacaoMap) {
  /* ── Paleta e Mapeamento de Cores por Tipo ── */
  const C = {
    primary: "#6c5dd3",
    secondary: "#ff754c",
    success: "#45b36b",
    warning: "#ffce73",
    danger: "#ef466f",
    info: "#3f8cff",
    muted: "#cfc8ff",
    text: "#ced4da",
    grid: "rgba(255,255,255,0.03)",
    border: "rgba(255,255,255,0.1)",
    palette: [
      "#6c5dd3",
      "#ffce73",
      "#ff754c",
      "#45b36b",
      "#3f8cff",
      "#ef466f",
      "#cfc8ff",
    ],
  };

  // Mapeamento fixo de Tipo -> Cor para consistência entre gráficos
  const typeColorMap = {
    ACAO: C.palette[0], // Roxo
    CRIPTOMOEDA: C.palette[1], // Amarelo/Ouro
    FII: C.palette[2], // Laranja
    RENDA_FIXA: C.palette[3], // Verde/Tesouro
    ETF: C.palette[4], // Azul
    OURO: C.palette[6], // Lilás
    OUTROS: C.palette[5], // Vermelho
  };

  const getFriendlyName = (t) =>
    ({
      ACAO: "Ações",
      CRIPTOMOEDA: "Cripto",
      FII: "FIIs",
      RENDA_FIXA: "Tesouro Direto",
      ETF: "ETFs",
    })[t] || "Outros";

  Chart.defaults.color = C.text;
  Chart.defaults.borderColor = C.grid;

  const fmt = (v) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(v);
  const compact = (v) =>
    new Intl.NumberFormat("en", { notation: "compact" }).format(v);
  const ctx = (id) => document.getElementById(id).getContext("2d");

  /* ── Gerador Determinístico ── */
  function createPRNG(seedString) {
    let h = 0;
    for (let i = 0; i < seedString.length; i++)
      h = (h << 5) - h + seedString.charCodeAt(i);
    h |= 0;
    return () => {
      h = (h * 1664525 + 1013904223) | 0;
      return (h >>> 0) / 4294967296;
    };
  }

  /* ── Suavização por média móvel (elimina tremulações) ── */
  function smooth(arr, w) {
    w = w || 8;
    return arr.map(function (_, i) {
      var s = Math.max(0, i - w);
      var e = Math.min(arr.length, i + w + 1);
      var slice = arr.slice(s, e);
      return (
        slice.reduce(function (a, b) {
          return a + b;
        }, 0) / slice.length
      );
    });
  }

  const portfolioSeed = JSON.stringify(ativos || []);
  const rng = createPRNG(portfolioSeed);
  const charts = {};

  function getGradient(cTarget, color, opacity) {
    opacity = opacity || "44";
    const grad = cTarget.createLinearGradient(0, 0, 0, 400);
    grad.addColorStop(0, color + opacity);
    grad.addColorStop(1, color + "00");
    return grad;
  }

  /* ── Plugin para Linha Pontilhada no Hover ── */
  const verticalDashedLine = {
    id: "verticalDashedLine",
    afterDraw: (chart) => {
      if (chart.tooltip?._active && chart.tooltip._active.length) {
        const activePoints = chart.tooltip._active;
        const ctx = chart.ctx;
        const x = activePoints[0].element.x;
        // Y no canvas inverte, então Math.min pega o ponto mais alto desenhado
        const minY = Math.min(...activePoints.map((p) => p.element.y));
        const bottomY = chart.scales.y.bottom;

        ctx.save();
        ctx.beginPath();
        ctx.moveTo(x, minY);
        ctx.lineTo(x, bottomY);
        ctx.lineWidth = 1.5;
        ctx.strokeStyle = "rgba(255, 255, 255, 0.4)";
        ctx.setLineDash([5, 5]);
        ctx.stroke();
        ctx.restore();
      }
    },
  };

  /* ── Sparklines ── */
  function sparkline(id, data, color) {
    const c = ctx(id);
    new Chart(c, {
      type: "line",
      data: {
        labels: Array(data.length).fill(""),
        datasets: [
          {
            data,
            borderColor: color,
            backgroundColor: getGradient(c, color, "22"),
            fill: true,
            borderWidth: 2,
            pointRadius: 0,
            tension: 0.4,
            cubicInterpolationMode: "monotone",
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, tooltip: { enabled: false } },
        scales: { x: { display: false }, y: { display: false } },
      },
    });
  }

  sparkline("sparkline1", [10, 15, 12, 18, 16, 22, 21, 25], C.success);
  sparkline("sparkline2", [60, 55, 65, 70, 68, 80, 75, 85], C.primary);
  sparkline("sparkline3", [40, 35, 45, 30, 38, 25, 30, 20], C.danger);

  /* ── Evolução do Patrimônio ── */
  function initLineChart() {
    const ctxLine = ctx("lineChart");
    let labels, dataLine, dataSelic, dataIbov;
    const DAYS = 365;

    if (ativos.length > 0) {
      const totalValue = ativos.reduce((s, a) => s + a.valorInvestido, 0);
      const dailySelic = Math.pow(1.1075, 1 / 252) - 1;
      let portfolio = totalValue * 0.8,
        benchmark = totalValue * 0.82,
        ibov = totalValue * 0.78;
      labels = [];
      dataLine = [];
      dataSelic = [];
      dataIbov = [];
      for (let i = DAYS; i >= 0; i--) {
        const date = new Date();
        date.setDate(date.getDate() - i);
        labels.push(
          date.toLocaleDateString("pt-BR", { day: "2-digit", month: "short" }),
        );
        benchmark *= 1 + dailySelic;
        dataSelic.push(benchmark);
        // Ruído reduzido: ±0.08% (era ±0.3%) para linhas mais suaves
        const noise = Math.sin(i * 0.1) * 0.004 + (rng() * 0.0016 - 0.0008);
        portfolio *= 1 + 0.0008 + noise;
        dataLine.push(portfolio);
        ibov *= 1 + 0.0004 + noise * 1.1;
        dataIbov.push(ibov);
      }
      const ratio = totalValue / dataLine[dataLine.length - 1];
      dataLine = smooth(
        dataLine.map((v) => v * ratio),
        10,
      );
      dataIbov = smooth(dataIbov, 10);
    } else {
      labels = Array.from({ length: DAYS }, (_, i) => {
        const d = new Date();
        d.setDate(d.getDate() - (DAYS - i));
        return d.toLocaleDateString("pt-BR", {
          day: "2-digit",
          month: "short",
        });
      });
      let p = 100000,
        s = 100000,
        b = 100000;
      dataLine = smooth(
        labels.map((_, i) => (p *= 1 + Math.sin(i / 10) * 0.008 + 0.001)),
        10,
      );
      dataSelic = labels.map(() => (s *= 1.00045));
      dataIbov = smooth(
        labels.map((_, i) => (b *= 1 + Math.cos(i / 15) * 0.01)),
        10,
      );
    }

    charts.lineChart = new Chart(ctxLine, {
      type: "line",
      data: {
        labels: labels,
        datasets: [
          {
            label: "Meus Ativos",
            data: dataLine,
            borderColor: C.primary,
            backgroundColor: getGradient(ctxLine, C.primary, "99"),
            fill: true,
            tension: 0.5,
            cubicInterpolationMode: "monotone",
            borderWidth: 3,
            pointRadius: 0,
            pointHoverRadius: 10,
            pointBackgroundColor: C.primary,
          },
          {
            label: "SELIC",
            data: dataSelic,
            borderColor: C.danger,
            fill: false,
            borderDash: [5, 5],
            borderWidth: 2,
            pointRadius: 0,
            tension: 0.4,
            cubicInterpolationMode: "monotone",
          },
          {
            label: "IBOVESPA",
            data: dataIbov,
            borderColor: C.warning,
            fill: false,
            borderWidth: 1.5,
            pointRadius: 0,
            tension: 0.5,
            cubicInterpolationMode: "monotone",
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { intersect: false, mode: "index" },
        plugins: {
          legend: {
            display: true,
            position: "top",
            align: "end",
            labels: {
              usePointStyle: true,
              pointStyle: "circle",
              color: C.text,
              boxWidth: 6,
              font: { size: 11, weight: "600" },
            },
          },
          tooltip: {
            backgroundColor: "rgba(13,13,18,0.9)",
            titleColor: C.text,
            bodyColor: "#fff",
            borderColor: C.border,
            borderWidth: 1,
            padding: 12,
            titleFont: { size: 12 },
            bodyFont: { size: 12 },
            callbacks: {
              label: (c) => c.dataset.label + ": " + fmt(c.parsed.y),
            },
          },
        },
        scales: {
          x: {
            display: true,
            ticks: {
              color: C.text,
              font: { size: 11 },
              maxRotation: 0,
              autoSkip: true,
              maxTicksLimit: 12,
            },
          },
          y: {
            border: { display: false },
            grid: { color: "rgba(255,255,255,0.03)" },
            ticks: { color: C.text, font: { size: 11 }, callback: compact },
          },
        },
      },
      plugins: [verticalDashedLine],
    });
  }

  /* ── Alocação por Tipo (Donut) ── */
  function initDonutChart() {
    let dLabels, dData, dColors;
    if (Object.keys(alocacaoMap).length > 0) {
      const finalMap = {};
      Object.entries(alocacaoMap).forEach(([k, v]) => {
        const name = getFriendlyName(k);
        finalMap[name] = (finalMap[name] || 0) + v;
      });
      dLabels = Object.keys(finalMap);
      dData = Object.values(finalMap);
      dColors = dLabels.map((label) => {
        const typeKey = Object.keys(alocacaoMap).find(
          (k) => getFriendlyName(k) === label,
        );
        return typeColorMap[typeKey] || typeColorMap["OUTROS"];
      });
    } else {
      dLabels = ["Ações", "Cripto", "FIIs", "Tesouro Direto", "Outros"];
      dData = [35000, 15000, 20000, 25000, 5000];
      dColors = [
        typeColorMap["ACAO"],
        typeColorMap["CRIPTOMOEDA"],
        typeColorMap["FII"],
        typeColorMap["RENDA_FIXA"],
        typeColorMap["OUTROS"],
      ];
    }

    new Chart(ctx("donutChart"), {
      type: "doughnut",
      data: {
        labels: dLabels,
        datasets: [
          {
            data: dData,
            backgroundColor: dColors,
            borderWidth: 0,
            hoverOffset: 15,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: "75%",
        plugins: {
          legend: {
            position: "bottom",
            labels: {
              usePointStyle: true,
              padding: 20,
              font: { size: 12 },
              color: C.text,
            },
          },
          tooltip: {
            backgroundColor: "rgba(13,13,18,0.9)",
            titleColor: C.text,
            bodyColor: "#fff",
            borderColor: C.border,
            borderWidth: 1,
            padding: 10,
            titleFont: { size: 12 },
            bodyFont: { size: 12 },
          },
        },
      },
    });
  }

  /* ── Top 10 ativos (Bar Chart) ── */
  function initBarChart() {
    let barAssets = [...ativos]
      .sort((a, b) => b.valorInvestido - a.valorInvestido)
      .slice(0, 10);
    const barLabels = barAssets.length
      ? barAssets.map((a) => a.ticker)
      : ["BTC", "AAPL", "VALE3", "PETR4", "ETH", "MXRF11", "IVVB11"];
    const barData = barAssets.length
      ? barAssets.map((a) => a.valorInvestido)
      : [45000, 18000, 15000, 12000, 10000, 8000, 5000];

    const barColors = barAssets.length
      ? barAssets.map((a) => typeColorMap[a.tipo] || typeColorMap["OUTROS"])
      : [
          typeColorMap["CRIPTOMOEDA"],
          typeColorMap["ACAO"],
          typeColorMap["ACAO"],
          typeColorMap["ACAO"],
          typeColorMap["CRIPTOMOEDA"],
          typeColorMap["FII"],
          typeColorMap["ETF"],
        ];

    const ctxBar = ctx("barChart");
    new Chart(ctxBar, {
      type: "bar",
      data: {
        labels: barLabels,
        datasets: [
          {
            label: "Valor Investido",
            data: barData,
            backgroundColor: barColors,
            borderRadius: 6,
            barThickness: 14,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: "rgba(13,13,18,0.9)",
            titleColor: C.text,
            bodyColor: "#fff",
            borderColor: C.border,
            borderWidth: 1,
            padding: 10,
            titleFont: { size: 12 },
            bodyFont: { size: 12 },
          },
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: C.text, font: { size: 11 } },
          },
          y: {
            grid: { color: "rgba(255,255,255,0.03)" },
            ticks: { color: C.text, font: { size: 11 }, callback: compact },
          },
        },
      },
    });
  }

  /* ── Comparativo por Categoria ── */
  function buildComparisonChart(canvasId, categoryAtivos, dummyAssets) {
    const cComp = ctx(canvasId);
    const top5 = categoryAtivos
      .sort((a, b) => b.valorInvestido - a.valorInvestido)
      .slice(0, 5);
    const compRng = createPRNG(canvasId + portfolioSeed);

    const datasets = top5.map((a, i) => {
      let val = a.valorInvestido;
      const color = C.palette[i % C.palette.length];
      // Ruído reduzido: ±0.4% (era ±1%) para linhas contínuas e suaves
      const data = smooth(
        Array.from(
          { length: 365 },
          () => (val *= 1 + (compRng() * 0.008 - 0.0038)),
        ),
        8,
      );
      data.reverse();
      return {
        label: a.ticker,
        data,
        borderColor: color,
        backgroundColor: "transparent",
        fill: false,
        borderWidth: 2,
        tension: 0.5,
        cubicInterpolationMode: "monotone",
        pointRadius: 0,
        pointHoverRadius: 8,
      };
    });

    const currentLength = datasets.length;
    dummyAssets.slice(currentLength).forEach((d, i) => {
      let val = d.base;
      const color = C.palette[(currentLength + i) % C.palette.length];
      const data = smooth(
        Array.from(
          { length: 365 },
          () => (val *= 1 + (compRng() * 0.006 - 0.0028)),
        ),
        8,
      );
      data.reverse();
      datasets.push({
        label: d.ticker,
        data,
        borderColor: color,
        backgroundColor: "transparent",
        fill: false,
        borderWidth: 1.5,
        pointRadius: 0,
        pointHoverRadius: 8,
        tension: 0.5,
        cubicInterpolationMode: "monotone",
      });
    });

    charts[canvasId] = new Chart(cComp, {
      type: "line",
      data: { labels: Array.from({ length: 365 }, (_, i) => i), datasets },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: "index", intersect: false },
        plugins: {
          legend: {
            display: true,
            position: "top",
            labels: {
              usePointStyle: true,
              pointStyle: "circle",
              color: C.text,
              font: { size: 11 },
              boxWidth: 8,
            },
          },
          tooltip: {
            mode: "index",
            intersect: false,
            backgroundColor: "rgba(13,13,18,0.9)",
            titleColor: C.text,
            bodyColor: "#fff",
            borderColor: C.border,
            borderWidth: 1,
            padding: 10,
            titleFont: { size: 12 },
            bodyFont: { size: 12 },
          },
        },
        scales: {
          x: { display: false },
          y: {
            grid: { color: "rgba(255,255,255,0.03)" },
            ticks: { color: C.text, font: { size: 11 }, callback: compact },
          },
        },
      },
      plugins: [verticalDashedLine],
    });
  }

  // Inicialização
  initLineChart();
  initDonutChart();
  initBarChart();

  buildComparisonChart(
    "cryptoComparisonChart",
    ativos.filter((a) => a.tipo === "CRIPTOMOEDA"),
    [
      { ticker: "BTC", base: 50000, type: "CRIPTOMOEDA" },
      { ticker: "ETH", base: 15000, type: "CRIPTOMOEDA" },
      { ticker: "SOL", base: 8000, type: "CRIPTOMOEDA" },
      { ticker: "BNB", base: 4000, type: "CRIPTOMOEDA" },
      { ticker: "ADA", base: 2000, type: "CRIPTOMOEDA" },
    ],
  );

  buildComparisonChart(
    "acaoComparisonChart",
    ativos.filter((a) => a.tipo === "ACAO"),
    [
      { ticker: "PETR4", base: 20000, type: "ACAO" },
      { ticker: "VALE3", base: 18000, type: "ACAO" },
      { ticker: "AAPL", base: 25000, type: "ACAO" },
      { ticker: "MSFT", base: 15000, type: "ACAO" },
      { ticker: "NVDA", base: 12000, type: "ACAO" },
    ],
  );

  window.updateChartPeriod = function (chartId, period) {
    const chart = charts[chartId];
    if (!chart) return;

    const periodMap = {
      day: "Intraday",
      week: "1S",
      month: "1M",
      "6months": "6M",
      year: "1A",
    };
    const pointsMap = {
      day: 24,
      week: 7,
      month: 30,
      "6months": 180,
      year: 365,
    };

    const dropdownBtn = document
      .querySelector(`#${chartId}`)
      .closest(".card")
      .querySelector(".dropdown-toggle");
    if (dropdownBtn) dropdownBtn.innerText = periodMap[period] || "Mês";

    if (chart.config.type === "line") {
      if (!chart.data.allLabels) chart.data.allLabels = [...chart.data.labels];
      if (!chart.data.allDatasetsData)
        chart.data.allDatasetsData = chart.data.datasets.map((d) => [
          ...d.data,
        ]);

      if (period === "day") {
        const hourlyLabels = [];
        const now = new Date();
        for (let i = 23; i >= 0; i--) {
          const h = new Date(now);
          h.setHours(now.getHours() - i);
          hourlyLabels.push(h.getHours().toString().padStart(2, "0") + ":00");
        }
        chart.data.labels = hourlyLabels;
        chart.data.datasets.forEach((d, idx) => {
          const lastValue =
            chart.data.allDatasetsData[idx][
              chart.data.allDatasetsData[idx].length - 1
            ];
          let tempVal = lastValue * 0.999;
          d.data = hourlyLabels.map(
            (h, i) =>
              (tempVal *=
                1 +
                (Math.sin(i * 0.2) * 0.0005 +
                  (Math.random() * 0.0004 - 0.0002))),
          );
          d.cubicInterpolationMode = "monotone";
          d.tension = 0.5;
        });
        chart.options.scales.x.maxTicksLimit = 8;
      } else {
        const points = pointsMap[period] || 30;
        chart.data.labels = chart.data.allLabels.slice(-points);
        chart.data.datasets.forEach((d, i) => {
          d.data = chart.data.allDatasetsData[i].slice(-points);
        });

        const ticksLimitMap = { week: 7, month: 10, "6months": 6, year: 12 };
        chart.options.scales.x.maxTicksLimit = ticksLimitMap[period] || 30;
      }

      chart.update();
    }
  };
})(window.__SAIKOO_ATIVOS__, window.__SAIKOO_ALOCACAO__);
