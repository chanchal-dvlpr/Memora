import '../mockVscode';
import * as assert from 'assert';
import { DashboardProvider, ArchitectureProvider, WorkProvider } from '../../sidebarProvider';

export async function runSidebarIntegrationTests() {
    console.log("-> Running SidebarIntegrationTest...");

    // Test 1: Dashboard Tree integrates with backend projects list
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] }
            ])
        };
        const provider = new DashboardProvider(mockClient as any);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 1);
        assert.strictEqual(children[0].label, "Proj1");
    }

    // Test 2: Architecture explorer details expand
    {
        const provider = new ArchitectureProvider();
        const categories = await provider.getChildren();
        assert.strictEqual(categories.length, 3);
        assert.strictEqual(categories[0].label, "📝 Active Design Decisions (ADRs)");

        const adrs = await provider.getChildren(categories[0]);
        assert.strictEqual(adrs.length, 2);
        assert.strictEqual(adrs[0].label, "ADR-001: Local-First Preservation");
    }

    // Test 3: Work activities details expand features/tasks/bugs
    {
        const provider = new WorkProvider();
        const categories = await provider.getChildren();
        assert.strictEqual(categories.length, 3);

        const features = await provider.getChildren(categories[0]);
        assert.strictEqual(features.length, 1);
        assert.strictEqual(features[0].label, "FEAT-101: Knowledge Graph Integration");
    }

    // Test 4: Dashboard tree handles backend connection drops gracefully
    {
        const mockClient = {
            getProjects: () => Promise.reject(new Error("Daemon Offline"))
        };
        const provider = new DashboardProvider(mockClient as any);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 1);
        assert.strictEqual(children[0].label, "Disconnected (Read-Only Mode)");
    }

    // Test 5: Dashboard tree renders multiple project nodes in multi-workspace folders
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] },
                { id: '2', name: 'Proj2', rootPath: '/p2', excludedPaths: [] }
            ])
        };
        const provider = new DashboardProvider(mockClient as any);
        const children = await provider.getChildren();
        assert.strictEqual(children.length, 2, "Should return multiple project nodes");
        assert.strictEqual(children[0].label, "Proj1");
        assert.strictEqual(children[1].label, "Proj2");
    }

    console.log("-> SidebarIntegrationTest passed.");
}
