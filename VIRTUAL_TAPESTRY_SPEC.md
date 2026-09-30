# Virtual Tapestry Data Specification & Frontend Integration Guide

> **Context**: This specification defines the Git repository structure and data contracts created by the **ZeThread** Android check-in app during **next.app devCon** (formerly Droidcon Berlin). Downstream static site generators (e.g. **Eleventy / 11ty**, **Astro**, **Vite**) or **Compose for Web (Kotlin Wasm/JS)** projects can ingest this structure directly to build an interactive, virtual crochet tapestry.

---

## 1. Architecture & Data Flow

```mermaid
flowchart TD
    subgraph Android App ["ZeThread (Android App)"]
        Cam[Crochet Patch Photo] --> Opt[Crop & Optimize]
        Meta[User Handle, Note, Coordinates X,Y] --> Payload[Generate Metadata]
        Opt --> API1["PUT Call 1: patches/{id}/patch.jpg"]
        Payload --> API2["PUT Call 2: patches/{id}/metadata.json"]
    end

    subgraph GitHub Repo ["GitHub Repository (Main Branch)"]
        API1 --> GitStore[Folder: patches/{patch_id}/]
        API2 --> GitStore
    end

    subgraph Downstream Web ["Virtual Tapestry Web App (11ty / Compose / Astro)"]
        GitStore --> CI[Build Step / Glob Loader]
        CI --> Data[Parse metadata.json + images]
        Data --> Grid[Virtual 2D Quilt Canvas]
        Grid --> Interactive[Click Tile -> Modal / Commit Link]
    end
```

### Why Dedicated Folders per Contribution?
Rather than appending to a single shared `tapestry.json` file (which causes HTTP `409 Conflict` errors during concurrent booth check-ins because the GitHub Contents API requires the parent blob SHA), each check-in writes to its own isolated subfolder:
```
patches/patch_{timestamp}_{handle}/
```
This guarantees strictly **append-only, conflict-free commits** directly from mobile devices on convention Wi-Fi.

---

## 2. Repository File Structure

Every contribution contains two files placed under the configured path prefix (`patches/` by default):

```text
<repository-root>/
├── patches/
│   ├── patch_20260228_142301_torvalds/
│   │   ├── patch.jpg           # Cropped crochet patch (JPEG, ~200-400KB)
│   │   └── metadata.json       # Structured contribution data & coordinates
│   ├── patch_20260228_142512_alex_dev/
│   │   ├── patch.jpg
│   │   └── metadata.json
│   └── ...
```

---

## 3. Metadata Schema (`metadata.json`)

Each `metadata.json` adheres to the following specification:

### JSON Schema (Draft 2020-12)
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "PatchMetadata",
  "type": "object",
  "required": ["id", "author", "coordinates", "timestamp", "image"],
  "properties": {
    "id": {
      "type": "string",
      "description": "Unique contribution slug: 'patch_{YYYYMMDD_HHmmss}_{handleSlug}'"
    },
    "author": {
      "type": "object",
      "required": ["handle", "email"],
      "properties": {
        "handle": {
          "type": "string",
          "description": "GitHub username or contributor nickname (without '@')"
        },
        "name": {
          "type": ["string", "null"],
          "description": "Public display name if resolved via GitHub API, or null"
        },
        "email": {
          "type": "string",
          "description": "Contributor contact or noreply email used in git commit"
        }
      }
    },
    "coordinates": {
      "type": "object",
      "required": ["x", "y"],
      "properties": {
        "x": {
          "type": "integer",
          "description": "Horizontal column index chosen by contributor (0-indexed, 0 = leftmost)"
        },
        "y": {
          "type": "integer",
          "description": "Vertical row index chosen by contributor (0-indexed, 0 = topmost)"
        }
      }
    },
    "note": {
      "type": ["string", "null"],
      "description": "Optional story, message, or stitch technique note"
    },
    "timestamp": {
      "type": "string",
      "format": "date-time",
      "description": "ISO-8601 UTC timestamp of submission (e.g. 2026-02-28T14:23:01Z)"
    },
    "image": {
      "type": "string",
      "description": "Relative filename of patch image within this directory ('patch.jpg')"
    },
    "image_commit_sha": {
      "type": ["string", "null"],
      "description": "Full 40-character SHA of the GitHub commit that created patch.jpg"
    }
  }
}
```

### Example `metadata.json` Payload
```json
{
  "id": "patch_20260228_142301_torvalds",
  "author": {
    "handle": "torvalds",
    "name": "Linus Torvalds",
    "email": "torvalds@users.noreply.github.com"
  },
  "coordinates": {
    "x": 4,
    "y": 2
  },
  "note": "Crocheted during the Kotlin Multiplatform keynote!",
  "timestamp": "2026-02-28T14:23:01Z",
  "image": "patch.jpg",
  "image_commit_sha": "76384fc99e6c6c33514c63713c54f20cac61b56d"
}
```

---

## 4. URL Resolution & GitHub Links

Frontend applications can construct direct links using the metadata attributes:

| Target | Formula / URL Pattern | Example |
| :--- | :--- | :--- |
| **Commit View** | `https://github.com/{owner}/{repo}/commit/{image_commit_sha}` | [Commit 76384fc](https://github.com/berlindroid/ZeThread/commit/76384fc99e6c6c33514c63713c54f20cac61b56d) |
| **Author Profile** | `https://github.com/{author.handle}` | [github.com/torvalds](https://github.com/torvalds) |
| **Avatar URL** | `https://github.com/{author.handle}.png?size=128` | [Avatar](https://github.com/torvalds.png?size=128) |
| **Raw Image (CDN)** | `https://raw.githubusercontent.com/{owner}/{repo}/{branch}/patches/{id}/patch.png` | Direct PNG raw link |
| **SSG Local Image** | `/patches/{id}/patch.png` (when copied to public assets) | Local bundled asset |

---

## 5. Downstream Implementation Guide

### Option A: Static Site Generator (11ty / Vite / Astro)

#### 1. Data Ingestion (e.g. `_data/tapestry.js` in Eleventy)
```javascript
// _data/tapestry.js
const fs = require('fs');
const path = require('path');
const { globSync } = require('glob');

module.exports = function () {
  const patchesDir = path.resolve(__dirname, '../patches');
  const metadataFiles = globSync('**/metadata.json', { cwd: patchesDir });

  const patches = metadataFiles.map((metaRelPath) => {
    const fullPath = path.join(patchesDir, metaRelPath);
    const raw = fs.readFileSync(fullPath, 'utf-8');
    const data = JSON.parse(raw);
    const folderName = path.dirname(metaRelPath);

    return {
      ...data,
      imagePath: `/patches/${folderName}/${data.image}`,
      commitUrl: data.image_commit_sha
        ? `https://github.com/berlindroid/ZeThread/commit/${data.image_commit_sha}`
        : null,
      profileUrl: `https://github.com/${data.author.handle}`,
      avatarUrl: `https://github.com/${data.author.handle}.png?size=96`
    };
  });

  // Calculate grid bounds
  const maxX = Math.max(0, ...patches.map(p => p.coordinates.x));
  const maxY = Math.max(0, ...patches.map(p => p.coordinates.y));

  return {
    items: patches,
    gridWidth: maxX + 1,
    gridHeight: maxY + 1
  };
};
```

#### 2. Layout Strategy: CSS Grid vs. 2D Pannable Canvas
- **KISS approach (Responsive Grid + Details Modal)**:
  Use CSS Grid with explicit `grid-column: ${x + 1}` and `grid-row: ${y + 1}`:
  ```css
  .tapestry-grid {
    display: grid;
    grid-template-columns: repeat(var(--cols), 120px);
    grid-template-rows: repeat(var(--rows), 120px);
    gap: 8px;
    padding: 24px;
  }
  .tapestry-tile {
    grid-column: calc(var(--x) + 1);
    grid-row: calc(var(--y) + 1);
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    transition: transform 0.2s ease;
  }
  .tapestry-tile:hover {
    transform: scale(1.08);
    z-index: 10;
    box-shadow: 0 8px 24px rgba(0,0,0,0.3);
  }
  ```
- **Pannable / Zoomable Canvas**:
  Wrap the grid in `@panzoom/panzoom` or a lightweight transform wrapper so users on desktop and mobile can drag, pinch-to-zoom, and explore the entire booth quilt like Google Maps.
- **Handling Coordinate Collisions**:
  If multiple contributors submit the same `(x, y)`:
  1. *Stacking*: Display multiple overlapping badges (e.g. `+2` indicator).
  2. *Latest Wins with History*: Show the most recent patch on top, clicking opens a carousel of all patches at that coordinate.

---

### Option B: Compose for Web / Kotlin Wasm

For a Compose for Web (`wasmJs` or `js`) implementation, reuse the Kotlin models directly:

```kotlin
@Serializable
data class PatchMetadata(
    val id: String,
    val author: PatchAuthor,
    val coordinates: PatchCoordinates,
    val note: String? = null,
    val timestamp: String,
    val image: String,
    @SerialName("image_commit_sha")
    val imageCommitSha: String? = null
)

@Serializable
data class PatchAuthor(
    val handle: String,
    val name: String? = null,
    val email: String
)

@Serializable
data class PatchCoordinates(
    val x: Int,
    val y: Int
)
```

Use `Modifier.pointerInput` with `detectTransformGestures { _, pan, zoom, _ -> ... }` to achieve fluid infinite 2D canvas navigation.

---

## 6. Prompt to Provide to an LLM for the Frontend Project

When creating the new frontend repository, paste the following prompt to the AI agent:

````markdown
I am building a web viewer for "ZeThread" (a collaborative crochet tapestry built at next.app devCon, formerly Droidcon Berlin).
The repository contains a `patches/` folder where each patch has:
- `patches/{patch_id}/patch.jpg` (the image of the crochet patch)
- `patches/{patch_id}/metadata.json` (author, coordinates {x, y}, note, timestamp, commit sha)

Please read `VIRTUAL_TAPESTRY_SPEC.md` for full schema details.

Requirements:
1. Parse all `patches/**/metadata.json` files and place them on a 2D coordinate grid (X = col, Y = row).
2. Allow users to pan and zoom across the virtual tapestry.
3. Clicking any patch opens a clean Material 3 modal with:
   - Patch image
   - Author handle (`@{handle}`), avatar, and link to GitHub profile
   - Coordinate badge `(x, y)`
   - Note / caption if present
   - Clickable commit hash badge linking to `https://github.com/{owner}/{repo}/commit/{image_commit_sha}`
4. Support empty grid slots with an aesthetic placeholder or booth wireframe motif.
5. Provide a fallback "List / Gallery" view for mobile users or quick browsing.
````
