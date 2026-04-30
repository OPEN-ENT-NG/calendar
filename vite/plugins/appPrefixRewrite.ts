import type { Plugin } from "vite";

export type AppPrefixRewritePluginOptions = {
  /**
   * Application name used in URLs (e.g. "calendar" for /calendar/public/...).
   */
  appName: string;
  /**
   * Optional matcher for paths that must be blocked in dev. Useful to prevent
   * serving stale build artifacts that can conflict with Vite modules.
   */
  blockedPattern?: RegExp;
  pluginName?: string;
};

/**
 * Rewrites production-like asset URLs to Vite-local paths during dev.
 *
 * Example:
 * - incoming: /calendar/public/template/top-menu.html
 * - rewritten: /template/top-menu.html
 *
 * Also blocks paths matched by `blockedPattern` (default: dist/ and
 * js/behaviours*) to avoid mixing old generated files with Vite-transformed
 * sources.
 */
export function appPrefixRewritePlugin({
  appName,
  blockedPattern = /^\/(dist\/|js\/behaviours)/,
  pluginName = "app-prefix-rewrite",
}: AppPrefixRewritePluginOptions): Plugin {
  const publicPrefix = `/${appName}/public/`;
  const publicBase = `/${appName}/public`;

  return {
    name: pluginName,
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const request = req as typeof req & { url?: string };
        if (request.url?.startsWith(publicPrefix)) {
          request.url = request.url.slice(publicBase.length);
        }
        if (request.url && blockedPattern.test(request.url)) {
          res.statusCode = 404;
          res.end();
          return;
        }
        next();
      });
    },
  };
}
