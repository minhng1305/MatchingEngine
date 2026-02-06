/**
 * Server Router for Multi-Server Matching Engine
 *
 * ARCHITECTURE (Kafka-based Dynamic Partitioning):
 * - Order Submission: ALL orders go to Ingress Server (8085) → Kafka "orders" topic (12 partitions)
 * - Kafka Consumer Groups: All matching servers (8080, 8081, 8082) subscribe to "orders" topic
 * - Partition Assignment: Kafka dynamically assigns partitions to servers
 *   - Server 1 (8080): Partitions 0-3
 *   - Server 2 (8081): Partitions 4-7
 *   - Server 3 (8082): Partitions 8-11
 * - Symbol Distribution: RANDOM (hash-based by Kafka)
 *   - hash(symbol) % 12 determines which partition (and thus which server) processes that symbol
 *   - Servers DON'T have pre-assigned symbols!
 *
 * PRODUCTION DEPLOYMENT:
 * - Single backend URL: All read operations go to ONE entry point (load balancer or single server)
 * - Each server maintains OrderBooks for ALL symbols (via Redis cache or local state)
 * - Frontend doesn't need to know which server processes which symbol
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

/**
 * PRODUCTION CONFIGURATION: Single backend URL for all operations
 * 
 * Since Kafka dynamically assigns partitions and symbols are hash-distributed,
 * we cannot predict which server handles which symbol. Instead:
 * - All read operations go to a single backend URL (load balancer or primary server)
 * - All servers maintain OrderBooks for ALL symbols (via Redis or state replication)
 * - Order submissions go to Ingress server (8085) → Kafka → All matching servers
 */
export const SERVER_CONFIGS: ServerConfig[] = [
    {
        port: 8080,
        baseUrl: getBaseUrl('REACT_APP_API_BASE_URL', 8080),
        wsPort: 8080,
        // ALL symbols - servers maintain OrderBooks for all symbols via Redis/cache
        symbols: ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NFLX', 
                  'NVDA', 'AMD', 'INTC', 'IBM', 'ORCL', 'CSCO', 'SAP',
                  'SNAP', 'BABA', 'TCEHY', 'ADOBE', 'CRM', 'TWTR'],
        kafkaTopics: ['orders'] // Single topic for all orders
    }
];

/**
 * DEVELOPMENT/LOCAL CONFIGURATION: Multiple servers (uncomment if running locally)
 * 
 * For local development with 3 servers + 1 ingress:
 * - Ingress (8085): Order submission only
 * - Server 1 (8080): Kafka partitions 0-3
 * - Server 2 (8081): Kafka partitions 4-7
 * - Server 3 (8082): Kafka partitions 8-11
 * 
 * Note: Even in local mode, symbol distribution is RANDOM (Kafka hash-based)
 * The configuration below is for load balancing read operations only.
 */
/*
export const SERVER_CONFIGS: ServerConfig[] = [
    {
        port: 8080,
        baseUrl: getBaseUrl('REACT_APP_SERVER1_URL', 8080),
        wsPort: 8080,
        symbols: ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NFLX', 
                  'NVDA', 'AMD', 'INTC', 'IBM', 'ORCL', 'CSCO', 'SAP',
                  'SNAP', 'BABA', 'TCEHY', 'ADOBE', 'CRM', 'TWTR'],
        kafkaTopics: ['orders']
    },
    {
        port: 8081,
        baseUrl: getBaseUrl('REACT_APP_SERVER2_URL', 8081),
        wsPort: 8081,
        symbols: ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NFLX', 
                  'NVDA', 'AMD', 'INTC', 'IBM', 'ORCL', 'CSCO', 'SAP',
                  'SNAP', 'BABA', 'TCEHY', 'ADOBE', 'CRM', 'TWTR'],
        kafkaTopics: ['orders']
    },
    {
        port: 8082,
        baseUrl: getBaseUrl('REACT_APP_SERVER3_URL', 8082),
        wsPort: 8082,
        symbols: ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NFLX', 
                  'NVDA', 'AMD', 'INTC', 'IBM', 'ORCL', 'CSCO', 'SAP',
                  'SNAP', 'BABA', 'TCEHY', 'ADOBE', 'CRM', 'TWTR'],
        kafkaTopics: ['orders']
    }
];
*/

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
