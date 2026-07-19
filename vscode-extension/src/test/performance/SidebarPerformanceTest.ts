import '../mockVscode';
import * as assert from 'assert';
import { DashboardProvider } from '../../sidebarProvider';

export async function runSidebarPerformanceTests() {
    console.log("-> Running SidebarPerformanceTest...");

    // Test 1: Initial tree creation latency
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] }
            ])
        };

        const start = process.hrtime.bigint();
        const provider = new DashboardProvider(mockClient as any);
        await provider.getChildren();
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;

        console.log(`   Initial tree creation: ${duration.toFixed(2)} ms`);
        assert.ok(duration < 100, "Initial tree creation must be < 100 ms");
    }

    // Test 2: Tree rendering latency with 1000 nodes (Scale/Load Test)
    {
        const largeProjectList = Array.from({ length: 1000 }, (_, i) => ({
            id: String(i),
            name: `Proj-${i}`,
            rootPath: `/path/${i}`,
            excludedPaths: []
        }));

        const mockClient = {
            getProjects: () => Promise.resolve(largeProjectList)
        };

        const provider = new DashboardProvider(mockClient as any);

        const start = process.hrtime.bigint();
        const children = await provider.getChildren();
        const end = process.hrtime.bigint();
        const duration = Number(end - start) / 1e6;

        console.log(`   1000-node tree rendering: ${duration.toFixed(2)} ms`);
        assert.ok(children.length === 1000);
        assert.ok(duration < 100, "1000-node tree rendering must be < 100 ms");
    }

    console.log("-> SidebarPerformanceTest passed.");
}
