import { useEffect, useRef, useCallback } from 'react';
import { createChart, IChartApi, ISeriesApi, LineData, Time } from 'lightweight-charts';
import { Trade } from '../../types';

interface Props {
  symbol: string;
  trades: Trade[];
  currentPrice: number;
}

export default function PriceChart({ symbol, trades, currentPrice }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<'Area'> | null>(null);

  const initChart = useCallback(() => {
    if (!containerRef.current) return;

    if (chartRef.current) {
      chartRef.current.remove();
    }

    const chart = createChart(containerRef.current, {
      layout: {
        background: { color: 'transparent' },
        textColor: '#6b7280',
        fontSize: 11,
        fontFamily: 'JetBrains Mono, monospace',
      },
      grid: {
        vertLines: { color: 'rgba(42, 52, 65, 0.3)' },
        horzLines: { color: 'rgba(42, 52, 65, 0.3)' },
      },
      crosshair: {
        vertLine: { color: '#3b82f6', width: 1, style: 2, labelBackgroundColor: '#3b82f6' },
        horzLine: { color: '#3b82f6', width: 1, style: 2, labelBackgroundColor: '#3b82f6' },
      },
      rightPriceScale: {
        borderColor: '#2a3441',
        scaleMargins: { top: 0.1, bottom: 0.1 },
      },
      timeScale: {
        borderColor: '#2a3441',
        timeVisible: true,
        secondsVisible: false,
      },
      handleScroll: true,
      handleScale: true,
    });

    const series = chart.addAreaSeries({
      lineColor: '#3b82f6',
      topColor: 'rgba(59, 130, 246, 0.2)',
      bottomColor: 'rgba(59, 130, 246, 0.0)',
      lineWidth: 2,
      priceLineVisible: true,
      priceLineColor: '#3b82f6',
      lastValueVisible: true,
    });

    chartRef.current = chart;
    seriesRef.current = series;

    const ro = new ResizeObserver((entries) => {
      const { width, height } = entries[0].contentRect;
      chart.applyOptions({ width, height });
    });
    ro.observe(containerRef.current);

    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    const cleanup = initChart();
    return () => {
      cleanup?.();
      chartRef.current?.remove();
      chartRef.current = null;
    };
  }, [initChart]);

  useEffect(() => {
    if (!seriesRef.current || trades.length === 0) return;

    const sorted = [...trades].sort(
      (a, b) => new Date(a.tradeTimestamp).getTime() - new Date(b.tradeTimestamp).getTime()
    );

    const data: LineData[] = [];
    let lastTs = 0;
    for (const t of sorted) {
      let ts = Math.floor(new Date(t.tradeTimestamp).getTime() / 1000);
      if (ts <= lastTs) {
        ts = lastTs + 1;
      }
      lastTs = ts;
      data.push({ time: ts as Time, value: t.price });
    }

    if (data.length > 0) {
      seriesRef.current.setData(data);
      chartRef.current?.timeScale().fitContent();
    }
  }, [trades, symbol]);

  useEffect(() => {
    if (!seriesRef.current || !currentPrice) return;
    const now = Math.floor(Date.now() / 1000) as Time;
    seriesRef.current.update({ time: now, value: currentPrice });
  }, [currentPrice]);

  return (
    <div className="panel flex flex-col h-full">
      <div className="panel-header flex items-center justify-between">
        <span>{symbol} Price</span>
        <span className="text-xs font-mono text-gray-300">${currentPrice.toFixed(2)}</span>
      </div>
      <div ref={containerRef} className="flex-1 min-h-0" />
    </div>
  );
}
