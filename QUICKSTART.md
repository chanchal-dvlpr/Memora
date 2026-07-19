# Memora Quick Start Guide

Get started with Memora in under 5 minutes. This guide walks you through registering a project, scanning its contents, generating an LLM handoff, and searching the semantic knowledge graph.

---

## Step 1: Start the Backend Server

Open a terminal window and run:

```bash
cd backend
./mvnw spring-boot:run
```
Once the server output prints `Started ContextEngineApplication`, the backend is ready.

---

## Step 2: Build and Link the CLI

Open a second terminal window and run:

```bash
cd cli
npm install && npm run build && npm link
```
Test that the CLI works by running:
```bash
memora status
```
*Expected Output:*
```
Memora Backend Connection Status:
----------------------------------------
Status: ONLINE
URL:    http://localhost:8080
```

---

## Step 3: Initialize Your Project Root

Go to your target development project directory (for example, the root of the Memora repository):

```bash
cd /Users/chanchalkumar/Documents/Codex/2026-07-12/we-are-starting-the-implementation-of
memora init
```
*Expected Output:*
```
Project initialized successfully!
ID:   <project-uuid>
Path: /Users/chanchalkumar/Documents/Codex/2026-07-12/we-are-starting-the-implementation-of
```

---

## Step 4: Index/Refresh the Project

Trigger a semantic scan of the workspace to build the Knowledge Graph dependencies:

```bash
memora refresh
```
*Expected Output:*
```
Project refreshed successfully!
Project ID:     <project-uuid>
Files Scanned:  2779
Snapshot Saved: Yes
```

---

## Step 5: Generate an LLM Handoff Context

Ask Memora to summarize the current state of your codebase (modified files, assumptions, active tasks, dependencies) to a markdown file for LLM ingestion:

```bash
memora handoff --stdout
```
*Expected Output:*
A compiled Markdown document outlining the metadata, registered features, codebase structure, and modified file list.

---

## Step 6: Search the Knowledge Graph

Query relations, decisions, or components inside your project:

```bash
memora search "Project"
```
*Expected Output:*
List of matching files, dependencies, decisions, or features corresponding to the query.

---

## Success!

You have completed the quickstart guide. For more advanced features or command configurations, consult [README.md](file:///Users/chanchalkumar/Documents/Codex/2026-07-12/we-are-starting-the-implementation-of/README.md).
