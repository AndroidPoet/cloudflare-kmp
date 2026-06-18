import { useConfig } from 'nextra-theme-docs'
import { useRouter } from 'next/router'

const Logo = () => (
  <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700 }}>
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M16.5 17H6.8a3.8 3.8 0 0 1-.5-7.57A5 5 0 0 1 16 9.2a3.4 3.4 0 0 1 .5 7.8Z"
        fill="#F38020"
      />
      <path
        d="m13.7 13.4.3-1c.06-.2-.05-.4-.26-.4H8.2a.2.2 0 0 1-.16-.32A4 4 0 0 1 15.4 12c.1.3.4.5.72.5h1.3c.2 0 .33.2.27.4l-.2.7c-.05.18-.22.3-.4.3h-3.1a.2.2 0 0 1-.2-.27Z"
        fill="#FBAD41"
      />
    </svg>
    <span>Cloudflare KMP</span>
  </span>
)

export default {
  logo: <Logo />,
  project: {
    link: 'https://github.com/AndroidPoet/cloudflare-kmp',
  },
  docsRepositoryBase: 'https://github.com/AndroidPoet/cloudflare-kmp/tree/main/website',
  color: {
    hue: 28,
    saturation: 90,
  },
  footer: {
    content: (
      <span>
        MIT © {new Date().getFullYear()}{' '}
        <a href="https://github.com/AndroidPoet/cloudflare-kmp" target="_blank" rel="noreferrer">
          Cloudflare KMP
        </a>
        . A type-safe Kotlin Multiplatform SDK and Worker gateway for Cloudflare.
      </span>
    ),
  },
  head: function useHead() {
    const { frontMatter } = useConfig()
    const { asPath } = useRouter()
    const pageTitle = frontMatter?.title
    const title = pageTitle ? `${pageTitle} – Cloudflare KMP` : 'Cloudflare KMP'
    const description =
      frontMatter?.description ??
      'Cloudflare KMP — a type-safe, coroutine-first Kotlin Multiplatform SDK and Worker gateway for Cloudflare D1, KV, R2 and realtime-style app backends on Android, iOS, JVM and Wasm.'
    const base = 'https://androidpoet.github.io/cloudflare-kmp'
    const path = asPath === '/' ? '' : asPath.split('?')[0].split('#')[0]
    const canonical = `${base}${path}`
    const ogImage = `${base}/favicon.svg`
    return (
      <>
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>{title}</title>
        <meta name="description" content={description} />
        <link rel="canonical" href={canonical} />
        <link rel="icon" href={`${base}/favicon.svg`} type="image/svg+xml" />
        <meta name="theme-color" content="#F38020" />
        <meta property="og:type" content="website" />
        <meta property="og:site_name" content="Cloudflare KMP" />
        <meta property="og:url" content={canonical} />
        <meta property="og:title" content={pageTitle ?? 'Cloudflare KMP'} />
        <meta property="og:description" content={description} />
        <meta property="og:image" content={ogImage} />
        <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:title" content={pageTitle ?? 'Cloudflare KMP'} />
        <meta name="twitter:description" content={description} />
        <meta name="twitter:image" content={ogImage} />
      </>
    )
  },
  sidebar: {
    defaultMenuCollapseLevel: 1,
  },
  toc: {
    backToTop: true,
  },
  navigation: {
    prev: true,
    next: true,
  },
  darkMode: true,
}
