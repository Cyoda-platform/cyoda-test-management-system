# Java Client Template

A **Gradle project** using **Spring Boot** with **Cyoda integration** for building scalable web clients with workflow-driven backend interactions.


## 🛠️ Getting Started

> ☕ **Java 21 Required**
> Make sure Java 21 is installed and set as the active version.

### 1. Clone the Project

```bash
git clone https://github.com/Cyoda-platform/java-client-template.git
cd java-client-template
```

### 2. ⚙️ Configure the Application

Configuration is managed via Spring Boot YAML files. For local development:

```bash
# Option 1: Create a local profile
# For example, create src/main/resources/application-local.yml with your settings
./gradlew runApp --args='--spring.profiles.active=local'

# Option 2: Use environment variables
export APP_CONFIG_CYODA_HOST=your-cyoda-host:8443
export APP_CONFIG_CYODA_CLIENT_ID=your-client-id
export APP_CONFIG_CYODA_CLIENT_SECRET=your-client-secret
```

### 3. 🧰 Run Workflow Import Tool

#### Option 1: Run via Gradle (recommended for local development)
```bash
./gradlew runApp -PmainClass=com.java_template.common.tool.WorkflowImportTool --args='--spring.profiles.active=local'
```

#### Option 2: Build and Run JAR (recommended for CI or scripting)
```bash
./gradlew bootJarWorkflowImport
java -jar build/libs/java-client-template-1.0-SNAPSHOT-workflow-import.jar --spring.profiles.active=local
```

### 4. ▶️ Run the Application

#### Option 1: Run via Gradle
```bash
./gradlew runApp --args='--spring.profiles.active=local'
```

#### Option 2: Run Manually After Build
```bash
./gradlew build
java -jar build/libs/java-client-template-1.0-SNAPSHOT.jar --spring.profiles.active=local
```

> Access the app: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
>
> **Note**: The default port is 8080 as configured in `src/main/resources/application.yml`. You can change this by setting the `server.port` property.

---

## 🏗️ Project Structure

This template follows a clear separation between **framework code** (that you don't modify) and **application code** (where you implement your business logic).

### `src/main/java/com/java_template/common/` - Framework Code (DO NOT MODIFY)

**Core Framework Components:**
- `auth/` – Authentication & token management for Cyoda integration
- `config/` – Configuration classes using Spring Boot's configuration management
- `dto/` – Data transfer objects including `EntityWithMetadata<T>` wrapper
- `grpc/` – gRPC client integration with Cyoda platform
- `repository/` – Data access layer for Cyoda REST API operations
- `service/` – `EntityService` interface and implementation for all Cyoda operations
- `serializer/` – Serialization framework with fluent APIs (`ProcessorSerializer`, `CriterionSerializer`)
- `tool/` – Utility tools like `WorkflowImportTool` for importing workflow configurations
- `util/` – Various utility functions and helpers
- `workflow/` – Core interfaces: `CyodaEntity`, `CyodaProcessor`, `CyodaCriterion`

> ⚠️ **IMPORTANT**: There is no need to modify anything in the `common/` directory. This is the framework code that provides all Cyoda integration.

### `src/main/java/com/java_template/application/` - Your Business Logic (CREATE AS NEEDED)

**Your Implementation Areas:**
- `controller/` – REST endpoints and HTTP API controllers
  - **`SearchController`** – Project-scoped search across all entities
  - **`GlobalSearchController`** – Global search across all projects
- `entity/` – Domain entities implementing `CyodaEntity` interface
- `processor/` – Workflow processors implementing `CyodaProcessor` interface
- `criterion/` – Workflow criteria implementing `CyodaCriterion` interface
- `service/` – Business logic services
  - **`SearchService`** – Cross-entity search engine (parallel searches with relevance scoring)

## 🔑 Core Concepts

### What is a CyodaEntity?
Domain objects that represent your business data. Must implement `CyodaEntity` interface and be placed in `application/entity/` directory.

### What is a CyodaProcessor?
Workflow components that handle business logic and entity transformations. **Critical limitation**: Cannot update the current entity being processed via EntityService.

### What is a CyodaCriterion?
Pure functions that evaluate conditions without side effects. Must not modify entities or have side effects.

### EntityWithMetadata<T> Pattern
Unified wrapper that includes both entity data and technical metadata (UUID, state, etc.). Used consistently across controllers, processors, and criteria.

## 🔄 Workflow Configuration

Workflows are defined using **finite-state machine (FSM)** JSON files placed in:
```
src/main/resources/workflow/$entity_name/version_$version/$entity_name.json
```

### Workflow Schema Reference
The workflow configuration schema is defined in:
```
src/main/resources/schema/common/statemachine/conf/WorkflowConfiguration.json
```
This schema defines the structure for workflow definitions, including states, transitions, processors, and criteria.

### Key Concepts
- **States and Transitions**: Define the workflow flow
- **Processors**: Handle business logic during transitions
- **Criteria**: Evaluate conditions to determine transition paths
- **Automatic Discovery**: Components are found via Spring `@Component` annotation

## 📚 Documentation and Examples

### Code Examples
- **`src/test/java/com/example/application/`** - Complete implementation examples for all components
  - `controller/` - REST controller patterns
  - `entity/` - Entity class implementations
  - `processor/` - Workflow processor examples  
  - `criterion/` - Workflow criteria examples
  - `patterns/` - Comprehensive patterns and anti-patterns guide

### Configuration Examples  
- **`llm_example/config/`** - Configuration templates and examples
  - `workflow/` - Workflow JSON configuration templates

### Documentation Files
- **`README.md`** - Complete project documentation (this file)
- **`CONTRIBUTING.md`** - Contributors guide and validation workflow
- **`usage-rules.md`** - Developer and AI agent guidelines
- **`.augment-guidelines`** - Project overview and development workflow
- **`llms.txt`** / **`llms-full.txt`** - AI-friendly documentation references

## 🔍 Unified Search

The TMS includes a **unified search engine** (`SearchService`) that performs parallel searches across all entity types and returns ranked results.

### Supported Search Domains

```
SearchService searches across 8 entity types:
├── Projects (name, description)
├── Suites (name, description)
├── Test Cases (name, description, steps)
├── Test Runs (name, description, status)
├── Test Run Cases (results, remarks)
├── Test Run Steps (actions, expected/actual results)
├── Defects (title, description, severity, status, link)
└── Reports (name, summary)
```

### API Endpoints

**Global Search** (across all projects, header search):
```
GET /api/v1/search?query=login&pageNumber=0&pageSize=10
```

**Project-Scoped Full Search** (pagination):
```
POST /api/v1/projects/{projectId}/search
{
  "query": "login button",
  "pageNumber": 0,
  "pageSize": 20
}
```

**Project-Scoped Quick Search** (header autocomplete, max 5 results):
```
GET /api/v1/projects/{projectId}/search/quick?query=login
```

### Response Format

```json
{
  "query": "login",
  "totalResults": 42,
  "resultCount": 20,
  "pageNumber": 0,
  "pageSize": 20,
  "results": [
    {
      "type": "defect",
      "id": "550e8400-...",
      "displayId": "DEF-01",
      "title": "Login button unresponsive",
      "description": "The login button doesn't respond on mobile",
      "metadata": "Critical - Open",
      "parentProjectId": "550e8400-...",
      "matchedFields": ["title", "description"],
      "score": 0.95,
      "externalLink": "https://jira.example.com/browse/TMS-42",
      "createdAt": "2024-02-15T10:00:00"
    }
  ],
  "executionTimeMs": 145,
  "typesSearched": 8
}
```

### Key Features

- **Case-Insensitive**: Searches automatically normalize query to lowercase
- **Multi-Field**: Searches across multiple fields per entity type
- **Relevance Scoring**: Results sorted by match quality (1.0 = exact, 0.5 = partial)
- **Parallel Execution**: All 8 entity types searched concurrently
- **Graceful Degradation**: Service failures don't break search; results from working services still returned
- **Tenant Isolation**: All searches scoped to single project
- **Pagination**: Supports configurable page size and number
- **Performance**: ~150ms response time typical for 100+ entity search

### Testing

**Unit Tests**: `SearchServiceTest.java` covers scoring, pagination, error handling

**E2E Tests**: `search.feature` validates end-to-end scenarios

Run tests:
```bash
# Unit tests only
./gradlew :apps:backend:test -k SearchService

# E2E tests (requires live Cyoda instance)
./gradlew :apps:backend:cucumberTest --tests "*search*"
```

### Implementation Details

- `SearchService` uses `CompletableFuture` for parallel searches (8-thread pool)
- Timeout: 30 seconds for all searches combined
- Page size limited: max 100 per request
- Results cached in memory only (no persistence)
- Supports empty queries (returns all entities)

---

## 📝 Quick Reference

### Key Concepts
- **Framework Code** (`common/`) - Never modify, provides all Cyoda integration
- **Application Code** (`application/`) - Your business logic implementation area
- **EntityWithMetadata<T>** - Unified wrapper pattern for all entity operations
- **EntityService** - Single interface for all Cyoda data operations
- **SearchService** - Parallel cross-entity search with relevance ranking

### Implementation Checklist
- ✅ Entities implement `CyodaEntity` with `getModelKey()` and `isValid()`
- ✅ Processors implement `CyodaProcessor` with `process()` and `supports()`
- ✅ Criteria implement `CyodaCriterion` with `check()` and `supports()`
- ✅ Use `@Component` annotation for Spring discovery
- ✅ Place workflow JSON files in `src/main/resources/workflow/$entity_name/version_$version/`
- ✅ Always reference `llm_example/` for implementation patterns

### Critical Limitations
- ❌ Never modify anything in `common/` directory
- ❌ Processors cannot update the current entity being processed
- ❌ Criteria must be pure functions without side effects
- ❌ No Java reflection usage allowed

> 📚 **See `llm_example/` directory for complete implementation examples, patterns, and configuration templates**

## 🚀 Getting Started

1. **Review Examples**: Start by exploring `llm_example/code/` for implementation patterns
2. **Create Entities**: Implement `CyodaEntity` in `application/entity/`
3. **Add Processors**: Implement `CyodaProcessor` in `application/processor/`
4. **Add Criteria**: Implement `CyodaCriterion` in `application/criterion/`
5. **Configure Workflows**: Create JSON files in `src/main/resources/workflow/`
6. **Build Controllers**: Create REST endpoints in `application/controller/`

## 🔧 Development Workflow

1. Review `llm_example/` directory for patterns before implementing new features
2. Follow established architectural patterns for processors, criteria, and serializers
3. Use `usage-rules.md` for detailed implementation guidelines
4. Run `./gradlew build` to generate required classes before development

**For Contributors:**

- See `CONTRIBUTING.md` for detailed guidelines

## Package Management

Always use appropriate package managers for dependency management:

1. **Use package managers** for all dependency operations instead of manually editing configuration files
2. **Exception**: Only edit package files directly for complex configurations that cannot be accomplished through package manager commands
3. **Generated Classes**: Ensure `build/generated-sources/js2p/org/cyoda/cloud/api/event` classes are available via `./gradlew build`
4. **Communication**: Use generated classes for all Cyoda integration
