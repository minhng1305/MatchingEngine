/**
 * Server Router for Multi-Server Matching Engine
 *
 * This service maps stock symbols to their corresponding backend servers.
 * Each server processes specific symbols as configured in backend application.properties.
 *
 * ARCHITECTURE:
 * - Order Submission: ALL orders go to Ingress Server (8085) → Kafka → Matching Servers
 * - Read Operations: Symbol-based routing to matching servers (8080/8081/8082)
 *   Each server maintains OrderBooks for symbols it processes
 *
 * CURRENT SETUP: Single server (8080) handling all symbols
 * Server 1 (8080): AAPL, GOOGL, MSFT, AMZN, TSLA, META, NFLX
 * 
 * NOTE: If you want to use multiple servers, update this configuration to match
 * your backend setup and ensure all servers are running on their respective ports.
 */

export interface ServerConfig {
    port: number;
    baseUrl: string;
    symbols: string[];
    wsPort: number;
    kafkaTopics: string[]; // For documentation/debugging
}

/**
 * Server configurations matching your backend setup
 * IMPORTANT: Keep this in sync with your application-server*.properties files
 * 
 * CURRENT CONFIGURATION: All symbols on port 8080
 * To use multiple servers, uncomment the other server configs and update symbols accordingly
 */
// Get base URL from environment variable, fallback to localhost for development
const getBaseUrl = (envVar: string, defaultPort: number): string => {
    const envUrl = process.env[envVar];
    if (envUrl) return envUrl;
    return `http://localhost:${defaultPort}`;
};

export const SERVER_CONFIGS: ServerConfig[] = [
    {
        port: 8080,
        baseUrl: getBaseUrl('REACT_APP_SERVER1_URL', 8080),
        wsPort: 8080,
        // ✅ MATCHES: application-server1.properties -> assigned-symbols=AAPL,GOOGL,MSFT,AMZN,TSLA,META,NFLX
        symbols: ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NFLX'],
        kafkaTopics: ['order-aapl', 'order-googl', 'order-msft', 'order-amzn',
            'order-tsla', 'order-meta', 'order-nflx']
    }
    // Uncomment below if you're running multiple servers:
    /*
    {
        port: 8081,
        baseUrl: 'http://localhost:8081',
        wsPort: 8081,
        // ⚠️ MUST match: application-server2.properties -> assigned-symbols
        symbols: ['NVDA', 'AMD', 'INTC', 'IBM', 'ORCL', 'CSCO', 'SAP'],
        kafkaTopics: ['order-nvda', 'order-amd', 'order-intc', 'order-ibm',
            'order-orcl', 'order-csco', 'order-sap']
    },
    {
        port: 8082,
        baseUrl: 'http://localhost:8082',
        wsPort: 8082,
        // ⚠️ MUST match: application-server3.properties -> assigned-symbols
        symbols: ['SNAP', 'BABA', 'TCEHY', 'ADOBE', 'CRM', 'TWTR'],
        kafkaTopics: ['order-snap', 'order-baba', 'order-tcehy', 'order-adobe',
            'order-crm', 'order-twtr']
    }
    */
];

class ServerRouter {
    private symbolToServerMap: Map<string, ServerConfig>;

    constructor() {
        // Build O(1) lookup map for symbol → server
        this.symbolToServerMap = new Map();

        SERVER_CONFIGS.forEach(config => {
            config.symbols.forEach(symbol => {
                this.symbolToServerMap.set(symbol.toUpperCase(), config);
            });
        });

        // Validation and logging
        this.logInitialization();
        this.validateConfiguration();
    }

    /**
     * Get server configuration for a specific symbol
     */
    getServerForSymbol(symbol: string): ServerConfig {
        const upperSymbol = symbol.toUpperCase();
        const server = this.symbolToServerMap.get(upperSymbol);

        if (!server) {
            console.error(`❌ Symbol ${symbol} not found in server configuration!`);
            console.error('Available symbols:', Array.from(this.symbolToServerMap.keys()));
            throw new Error(`Unknown symbol: ${symbol}`);
        }

        return server;
    }

    /**
     * Get API base URL for symbol-specific requests
     */
    getApiBaseUrl(symbol: string): string {
        return this.getServerForSymbol(symbol).baseUrl;
    }

    /**
     * Get WebSocket URL for real-time updates
     */
    getWebSocketUrl(symbol: string): string {
        const server = this.getServerForSymbol(symbol);
        return `http://localhost:${server.wsPort}`;
    }

    /**
     * Get all server URLs for aggregated requests
     */
    getAllServerUrls(): string[] {
        return SERVER_CONFIGS.map(config => config.baseUrl);
    }

    /**
     * Get Kafka topic name for a symbol (for debugging)
     */
    getExpectedKafkaTopic(symbol: string): string {
        return `order-${symbol.toLowerCase()}`;
    }

    /**
     * Validate configuration matches backend setup
     */
    private validateConfiguration(): void {
        const errors: string[] = [];
        const allSymbols = new Set<string>();
        let totalSymbols = 0;

        SERVER_CONFIGS.forEach((config, idx) => {
            // Check for duplicate symbols across servers
            config.symbols.forEach(symbol => {
                const upper = symbol.toUpperCase();
                if (allSymbols.has(upper)) {
                    errors.push(`❌ Duplicate symbol ${symbol} found in Server ${idx + 1}`);
                }
                allSymbols.add(upper);
                totalSymbols++;
            });

            // Verify Kafka topics match symbols
            if (config.kafkaTopics.length !== config.symbols.length) {
                errors.push(`⚠️ Server ${config.port}: ${config.symbols.length} symbols but ${config.kafkaTopics.length} topics`);
            }
        });

        if (errors.length > 0) {
            console.error('🚨 Configuration Errors:');
            errors.forEach(err => console.error(err));
            throw new Error('Server configuration validation failed!');
        }

        console.log(`✅ Configuration valid: ${totalSymbols} symbols across ${SERVER_CONFIGS.length} servers`);
    }

    /**
     * Log initialization details
     */
    private logInitialization(): void {
        console.log('╔═══════════════════════════════════════════════════════╗');
        console.log('║      🚀 Server Router Initialized                    ║');
        console.log('╠═══════════════════════════════════════════════════════╣');

        SERVER_CONFIGS.forEach(config => {
            console.log(`║  Server ${config.port}: ${config.symbols.length} symbols`);
            console.log(`║    ${config.symbols.join(', ')}`);
        });

        console.log('╚═══════════════════════════════════════════════════════╝');
    }

    /**
     * Get statistics about server distribution
     */
    getStats(): {
        totalSymbols: number;
        serverCount: number;
        distribution: { [key: string]: number };
    } {
        const distribution: { [key: string]: number } = {};
        SERVER_CONFIGS.forEach(config => {
            distribution[`Server ${config.port}`] = config.symbols.length;
        });

        return {
            totalSymbols: Array.from(this.symbolToServerMap.keys()).length,
            serverCount: SERVER_CONFIGS.length,
            distribution
        };
    }

    /**
     * Check if a symbol is registered
     */
    hasSymbol(symbol: string): boolean {
        return this.symbolToServerMap.has(symbol.toUpperCase());
    }

    /**
     * Get all registered symbols
     */
    getAllSymbols(): string[] {
        return Array.from(this.symbolToServerMap.keys()).sort();
    }
}

// Export singleton instance
export const serverRouter = new ServerRouter();

// Export convenience functions
export const getServerForSymbol = (symbol: string) => serverRouter.getServerForSymbol(symbol);
export const getApiBaseUrl = (symbol: string) => serverRouter.getApiBaseUrl(symbol);
export const getWebSocketUrl = (symbol: string) => serverRouter.getWebSocketUrl(symbol);
export const getAllServerUrls = () => serverRouter.getAllServerUrls();
export const hasSymbol = (symbol: string) => serverRouter.hasSymbol(symbol);
export const getAllSymbols = () => serverRouter.getAllSymbols();
