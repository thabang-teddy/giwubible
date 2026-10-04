<?php

declare(strict_types=1);

namespace App\Services;

use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Throwable;

/**
 * Reads the newest GitHub release (pre-releases included — CI publishes
 * staging builds as pre-releases) so the download page can show its notes and
 * send users straight to the attached APK/EXE.
 */
class GithubReleases
{
    private const CACHE_KEY = 'github.latest_release';

    private const TTL_OK = 600;

    private const TTL_FAILED = 60;

    /** Platform => file extension of the release asset. */
    public const PLATFORMS = ['android' => '.apk', 'windows' => '.exe'];

    public function repo(): string
    {
        return (string) config('services.github.repo');
    }

    public function repoUrl(): string
    {
        return 'https://github.com/'.$this->repo();
    }

    /**
     * @return array{tag:string,name:string,notes:string,url:string,published_at:?string,prerelease:bool,assets:array<string,array{name:string,size:int,url:string}>}|null
     */
    public function latest(): ?array
    {
        $cached = Cache::get(self::CACHE_KEY);
        if ($cached !== null) {
            // `false` is the cached "GitHub failed" marker, so a flaky API is not hammered.
            return $cached ?: null;
        }

        $release = $this->fetch();
        Cache::put(self::CACHE_KEY, $release ?? false, $release ? self::TTL_OK : self::TTL_FAILED);

        return $release;
    }

    public function assetUrl(string $platform): ?string
    {
        return $this->latest()['assets'][$platform]['url'] ?? null;
    }

    private function fetch(): ?array
    {
        try {
            $response = Http::acceptJson()
                ->withHeaders(['X-GitHub-Api-Version' => '2022-11-28'])
                ->withUserAgent('giwubible-download-page')
                ->timeout(5)
                ->get("https://api.github.com/repos/{$this->repo()}/releases", ['per_page' => 1]);
        } catch (Throwable $e) {
            report($e);

            return null;
        }

        $release = $response->successful() ? ($response->json()[0] ?? null) : null;
        if (! is_array($release)) {
            return null;
        }

        return [
            'tag' => $release['tag_name'],
            'name' => $release['name'] ?: $release['tag_name'],
            'notes' => (string) ($release['body'] ?? ''),
            'url' => $release['html_url'],
            'published_at' => $release['published_at'] ?? null,
            'prerelease' => (bool) $release['prerelease'],
            'assets' => $this->pickAssets($release['assets'] ?? []),
        ];
    }

    private function pickAssets(array $assets): array
    {
        $picked = [];
        foreach (self::PLATFORMS as $platform => $extension) {
            foreach ($assets as $asset) {
                if (str_ends_with(strtolower($asset['name']), $extension)) {
                    $picked[$platform] = [
                        'name' => $asset['name'],
                        'size' => (int) $asset['size'],
                        'url' => $asset['browser_download_url'],
                    ];
                    break;
                }
            }
        }

        return $picked;
    }
}
