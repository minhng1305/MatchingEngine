# Secrets Configuration

This folder contains sensitive configuration files that should **NOT** be committed to version control.

## Setup Instructions

1. Copy the example file to create your actual secret file:
   ```bash
   cp secrets.properties.example secrets.properties
   ```

2. Edit the `secrets.properties` file and replace the placeholder values with your actual credentials.

3. The `.gitignore` file is configured to exclude the actual secret file from version control.

## Security Best Practices

- ✅ Store secrets outside the codebase (this folder is excluded from git)
- ✅ Use different credentials for different environments (dev, staging, prod)
- ✅ Never commit actual secret files to version control
- ✅ Use strong, unique passwords and secrets
- ✅ Regularly rotate secrets and credentials
- ✅ In production, consider using environment variables or a secrets management service (AWS Secrets Manager, HashiCorp Vault, etc.)

## Files in this Directory

- `secrets.properties` - Shared secrets for all servers (not in git)
- `secrets.properties.example` - Example/template file (safe to commit)

**Note:** All three servers share the same credentials, so a single file is used. If you need different credentials per server in the future, you can create separate files (e.g., `secrets-server1.properties`, etc.) and update the `spring.config.import` property in each application properties file accordingly.

## How It Works

The Spring Boot application properties files reference these external secret files using `spring.config.import`. The application will load the secrets from these files at runtime.

## Production Recommendations

For production deployments, consider:
1. **Environment Variables**: Use environment variables instead of files
2. **Secrets Management Services**: AWS Secrets Manager, HashiCorp Vault, Azure Key Vault
3. **CI/CD Integration**: Inject secrets during deployment pipelines
4. **File Permissions**: Ensure secret files have restricted permissions (e.g., `chmod 600`)
