<?php

declare(strict_types=1);

namespace App\Http\Controllers\Web;

use App\Http\Controllers\Controller;
use App\Services\GithubReleases;
use Illuminate\Http\RedirectResponse;
use Inertia\Inertia;
use Inertia\Response;

class DownloadController extends Controller
{
    public function __construct(private readonly GithubReleases $releases) {}

    public function __invoke(): Response
    {
        return Inertia::render('Download', [
            'release' => $this->releases->latest(),
            'repoUrl' => $this->releases->repoUrl(),
        ]);
    }

    /**
     * Send the browser to the release asset on GitHub. GitHub serves it as an
     * attachment, so the download starts without the user leaving the page.
     */
    public function asset(string $platform): RedirectResponse
    {
        abort_unless(array_key_exists($platform, GithubReleases::PLATFORMS), 404);

        $url = $this->releases->assetUrl($platform);
        abort_if($url === null, 404, 'No build of that app is published yet.');

        return redirect()->away($url);
    }
}
