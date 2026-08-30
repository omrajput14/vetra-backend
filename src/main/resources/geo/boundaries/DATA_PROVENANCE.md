# VETRA Administrative Boundary Data Provenance & Specification

**Authoritative Documentation on Geographic Vector Assets for Epidemiological Surveillance**
**Date of Download & Audit:** August 30, 2026  
**Coordinate Reference System:** WGS84 (EPSG:4326)  
**Standard Format:** RFC 7946 GeoJSON `FeatureCollection`

---

## 1. Executive Summary & Verification Matrix

The VETRA Government Command Center utilizes multi-tier administrative boundary vector datasets staged in the Spring Boot backend (`vetra-backend/src/main/resources/geo/boundaries/`). These boundary layers serve as the spatial reference frames for state, district, and taluka epidemiological cluster isolation.

| Level | Name | Verified Feature Count | Geometry Type | Data Source | License |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ADM1** | Maharashtra State | **1 Feature** | `MultiPolygon` | **geoBoundaries (v5.0)** (Survey of India) | [CC BY 2.5 IN](https://creativecommons.org/licenses/by/2.5/in/) |
| **ADM2** | Maharashtra Districts | **36 Features** | `Polygon` / `MultiPolygon` | **DataMeet Community Geodata** (Census of India / ECI) | [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1.0/) |
| **ADM3** | Maharashtra Talukas / Sub-Districts | **357 Features** | `Polygon` / `MultiPolygon` | **DataMeet Community Geodata** (Census of India) | [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1.0/) |
| **Combined** | All Boundaries | **394 Features** | `Polygon` / `MultiPolygon` | geoBoundaries / DataMeet | CC BY 2.5 IN & ODbL 1.0 |

---

## 2. Dataset Provenance & Source Details

### ADM1: State Boundary
- **Source:** [geoBoundaries v5.0](https://www.geoboundaries.org/) (ISO: `IND-ADM1`, ShapeID: `IN-MH`)
- **Underlying Authority:** Survey of India
- **Download Date:** August 30, 2026
- **License:** Creative Commons Attribution 2.5 India (CC BY 2.5 IN)
- **Bounding Box:** `[72.635, 15.602, 80.898, 22.028]`
- **Feature Count:** Exactly 1 feature (`administrativeLevel: "STATE"`, `name: "Maharashtra"`).

### ADM2: District Boundaries
- **Source:** [DataMeet India Community Geodata - Maps](https://github.com/datameet/maps/tree/master/Districts) (Census 2011 & Election Commission of India spatial base)
- **Download Date:** August 30, 2026
- **License:** Open Database License (ODbL) 1.0
- **Feature Count:** Exactly 36 features representing all 36 administrative districts of Maharashtra:
  1. Ahmednagar
  2. Akola
  3. Amravati
  4. Aurangabad
  5. Beed
  6. Bhandara
  7. Buldhana
  8. Chandrapur
  9. Dhule
  10. Gadchiroli
  11. Gondia
  12. Hingoli
  13. Jalgaon
  14. Jalna
  15. Kolhapur
  16. Latur
  17. Mumbai
  18. Mumbai Suburban
  19. Nagpur
  20. Nanded
  21. Nandurbar
  22. Nashik
  23. Osmanabad
  24. Palghar
  25. Parbhani
  26. Pune
  27. Raigad
  28. Ratnagiri
  29. Sangli
  30. Satara
  31. Sindhudurg
  32. Solapur
  33. Thane
  34. Wardha
  35. Washim
  36. Yavatmal

### ADM3: Taluka / Sub-District Boundaries
- **Source:** [DataMeet India Community Geodata - Sub-Districts](https://github.com/datameet/maps) (Census of India spatial base)
- **Download Date:** August 30, 2026
- **License:** Open Database License (ODbL) 1.0
- **Feature Count:** Exactly 357 features strictly validated to be geographically contained within the 36 Maharashtra district polygons.

---

## 3. RFC 7946 GeoJSON Integrity & Topology Audit

An automated topology validation pipeline was executed on all files with the following strict checks:
1. **FeatureCollection & Feature Structure:** Root elements are standard `FeatureCollection` objects; all child items have `"type": "Feature"`, valid `"geometry"`, and non-empty `"properties"`.
2. **Geometry Type & Linear Ring Closure:** All geometries are `Polygon` or `MultiPolygon`. Every linear ring contains $\ge 4$ positions where `ring[0] == ring[-1]` (closed rings).
3. **Coordinate Reference System:** WGS84 (`[longitude, latitude]` format in degrees within Maharashtra bounding box `[72.0 - 81.5, 15.0 - 22.5]`).
4. **Mandatory Property Schema:**
   - `name` (String, non-empty)
   - `administrativeLevel` (`STATE`, `DISTRICT`, or `TALUKA`)
   - `state` (`Maharashtra`)
   - `district` (non-null String matching parent district for all `TALUKA` features)
   - `boundaryId` (Unique administrative identifier)
   - `source` (Dataset provenance attribution)

### Out-of-State / Cross-Border Filtering
During validation against the 36 district geometry boundaries, cross-border slivers or non-Maharashtra sub-districts present in raw pan-India datasets (specifically **Nizar** in Tapi District, Gujarat) were excluded rather than having coordinates mutated or falsely assigned to Maharashtra.

---

## 4. Backend & Frontend Architectural Architecture

1. **Source of Truth:**
   - The Spring Boot backend (`AdministrativeBoundaryService.java`) parses and caches boundaries in-memory during application startup (`@PostConstruct`).
   - Endpoint `GET /api/v1/geo/boundaries` returns RFC 7946 `FeatureCollection` with query parameters `level`, `district`, `state`.
   - Endpoint `GET /api/v1/geo/districts` returns the 36 dynamically extracted district names directly from the loaded GeoJSON properties.

2. **Frontend Dynamic Sourcing:**
   - No district lists or geographic coordinates are hardcoded into React/TypeScript constants.
   - The frontend queries `gisService.getDistricts()` and `gisService.getAdministrativeBoundaries()`.
   - The UI filter dropdowns and boundary renderers consume dynamic GeoJSON properties.

3. **Multi-Scale Visualization & Performance:**
   - **Statewide View (Zoom < 9):** State border (`#1E5C97`, weight 2.2) and District borders (`#475569`, dashed, weight 1.2) are active. Taluka boundaries default to OFF to maximize rendering performance and cartographic legibility.
   - **Local / District View (Zoom $\ge$ 9 or District Selected):** Taluka boundaries automatically render at high resolution (`#94A3B8`, dashed, weight 0.8) with interactive tooltips.
   - **User Manual Override:** Users can toggle State, District, and Taluka layers on or off at any zoom level via the GisFilterBar controls.
