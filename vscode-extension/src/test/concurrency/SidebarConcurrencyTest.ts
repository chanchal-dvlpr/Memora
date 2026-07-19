import '../mockVscode';
import * as assert from 'assert';
import { DashboardProvider } from '../../sidebarProvider';

export async function runSidebarConcurrencyTests() {
    console.log("-> Running SidebarConcurrencyTest...");

    // Test 1: Concurrent getChildren calls
    {
        const mockClient = {
            getProjects: () => Promise.resolve([
                { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] }
            ])
        };

        const provider = new DashboardProvider(mockClient as any);
        const promises = Array.from({ length: 50 }, () => provider.getChildren());
        const results = await Promise.all(promises);
        results.forEach(children => {
            assert.strictEqual(children.length, 1);
            assert.strictEqual(children[0].label, "Proj1");
        });
    }

    // Test 2: Project list shifts (add/remove) concurrently
    {
        let projects = [
            { id: '1', name: 'Proj1', rootPath: '/p1', excludedPaths: [] }
        ];

        const mockClient = {
            getProjects: () => Promise.resolve(projects)
        };

        const provider = new DashboardProvider(mockClient as any);

        // Concurrently query children while projects array shifts dynamically
        const p1 = provider.getChildren();
        projects.push({ id: '2', name: 'Proj2', rootPath: '/p2', excludedPaths: [] });
        const p2 = provider.getChildren();
        projects.pop();
        const p3 = provider.getChildren();

        const [r1, r2, r3] = await Promise.all([p1, p2, p3]);
        assert.ok(r1.length > 0);
        assert.ok(r2.length > 0);
        assert.ok(r3.length > 0);
    }

    console.log("-> SidebarConcurrencyTest passed.");
}
