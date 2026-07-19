import '../mockVscode';
import * as assert from 'assert';
import { DashboardProvider } from '../../sidebarProvider';

export async function runSidebarMemoryTests() {
    console.log("-> Running SidebarMemoryTest...");

    const mockClient = {
        getProjects: () => Promise.resolve([
            { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] }
        ])
    };

    // Test 1: 100 Tree creation, expansion, and query cycles
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 100; i++) {
            const provider = new DashboardProvider(mockClient as any);
            const children = await provider.getChildren();
            assert.strictEqual(children.length, 1);
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 100 Tree cycles: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5);
    }

    // Test 2: 500 Tree creation, expansion, and query cycles (Scale Check)
    {
        const memBefore = process.memoryUsage().heapUsed;
        for (let i = 0; i < 500; i++) {
            const provider = new DashboardProvider(mockClient as any);
            const children = await provider.getChildren();
            assert.ok(children.length > 0);
        }
        const memAfter = process.memoryUsage().heapUsed;
        const drift = (memAfter - memBefore) / 1024 / 1024;
        console.log(`   Memory drift after 500 Tree cycles: ${drift.toFixed(2)} MB`);
        assert.ok(drift < 5, "500 Tree creation cycles must remain under memory thresholds");
    }

    console.log("-> SidebarMemoryTest passed.");
}
