<?php

namespace Tests\Feature;

use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

class DownloadTest extends TestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        Cache::flush();
    }

    private function fakeRelease(array $assets): void
    {
        Http::fake(['api.github.com/*' => Http::response([[
            'tag_name' => 'v1.0.0-staging.7',
            'name' => 'Staging v1.0.0-staging.7',
            'body' => "## Changes\n* fix: thing",
            'html_url' => 'https://github.com/thabang-teddy/giwubible/releases/tag/v1.0.0-staging.7',
            'published_at' => '2026-10-01T10:00:00Z',
            'prerelease' => true,
            'assets' => $assets,
        ]])]);
    }

    public function test_page_receives_latest_release_notes_and_assets(): void
    {
        $this->fakeRelease([[
            'name' => 'giwubible-v1.apk',
            'size' => 1234,
            'browser_download_url' => 'https://github.com/x/y/releases/download/v1/giwubible-v1.apk',
        ]]);

        $this->get('/download')
            ->assertOk()
            ->assertInertia(fn ($page) => $page
                ->component('Download')
                ->where('release.tag', 'v1.0.0-staging.7')
                ->where('release.assets.android.name', 'giwubible-v1.apk')
                ->missing('release.assets.windows')
                ->has('repoUrl'));
    }

    public function test_page_still_renders_when_github_is_unreachable(): void
    {
        Http::fake(['api.github.com/*' => Http::response([], 500)]);

        $this->get('/download')
            ->assertOk()
            ->assertInertia(fn ($page) => $page->where('release', null));
    }

    public function test_asset_route_redirects_to_github(): void
    {
        $url = 'https://github.com/x/y/releases/download/v1/giwubible-v1.apk';
        $this->fakeRelease([['name' => 'giwubible-v1.apk', 'size' => 1, 'browser_download_url' => $url]]);

        $this->get('/download/android')->assertRedirect($url);
    }

    public function test_asset_route_404s_for_missing_build_or_unknown_platform(): void
    {
        $this->fakeRelease([]);

        $this->get('/download/windows')->assertNotFound();
        $this->get('/download/linux')->assertNotFound();
    }
}
