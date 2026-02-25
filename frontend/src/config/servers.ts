export interface ServerConfig {
  id: string;
  baseUrl: string;
  wsUrl: string;
  symbols: string[];
}

const server1Url = import.meta.env.VITE_SERVER1_URL || 'http://localhost:8080';
const server2Url = import.meta.env.VITE_SERVER2_URL || 'http://localhost:8081';
const server3Url = import.meta.env.VITE_SERVER3_URL || 'http://localhost:8082';

export const INGRESS_URL = import.meta.env.VITE_INGRESS_BASE_URL || 'http://localhost:8085/api';

export const SERVERS: ServerConfig[] = [
  {
    id: 'server1',
    baseUrl: `${server1Url}/api`,
    wsUrl: `${server1Url}/ws`,
    symbols: ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NFLX'],
  },
  {
    id: 'server2',
    baseUrl: `${server2Url}/api`,
    wsUrl: `${server2Url}/ws`,
    symbols: ['NVDA', 'AMD', 'INTC', 'IBM', 'ORCL', 'CSCO', 'SAP'],
  },
  {
    id: 'server3',
    baseUrl: `${server3Url}/api`,
    wsUrl: `${server3Url}/ws`,
    symbols: ['ADOBE', 'CRM', 'TWTR', 'SNAP', 'BABA', 'TCEHY'],
  },
];

const symbolServerMap = new Map<string, ServerConfig>();
SERVERS.forEach((server) => {
  server.symbols.forEach((symbol) => symbolServerMap.set(symbol, server));
});

export function getServerForSymbol(symbol: string): ServerConfig {
  return symbolServerMap.get(symbol.toUpperCase()) ?? SERVERS[0];
}

export function getAllSymbols(): string[] {
  return SERVERS.flatMap((s) => s.symbols);
}

export const DEFAULT_API = import.meta.env.VITE_API_BASE_URL || `${server1Url}/api`;
