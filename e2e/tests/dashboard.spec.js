import { test, expect } from '../fixtures/test.js'

test('shows the dashboard and complete forecast horizon', async ({ dashboardPage }) => {
  await dashboardPage.open()

  await expect(dashboardPage.heading).toBeVisible()
  await dashboardPage.openMonthlyForecast()
  await expect(dashboardPage.monthlyRows).toHaveCount(50)
  await dashboardPage.openAnnualAnalysis()
  await expect(dashboardPage.annualRows).toHaveCount(5)
  await expect(dashboardPage.annualTable).toContainText('2030')
})

test('keeps dashboard cards and route navigation aligned across mobile and desktop widths', async ({ dashboardPage }) => {
  const { page } = dashboardPage
  await dashboardPage.open()

  for (const width of [375, 390, 430, 1024, 1280, 1440]) {
    await page.setViewportSize({ width, height: 900 })
    await expect(page.locator('.outlook-hero')).toBeVisible()
    const layout = await page.evaluate(() => {
      const bounds = (selector) => {
        const element = document.querySelector(selector)
        const rect = element.getBoundingClientRect()
        return { left: rect.left, right: rect.right, width: rect.width }
      }
      const mobile = window.matchMedia('(max-width: 767px)').matches
      const activeNavigation = mobile
        ? document.querySelectorAll('.mobile-bottom-nav .mobile-nav-link.active').length
        : document.querySelectorAll('.side-nav .nav-link.active').length
      const desktopLabels = [...document.querySelectorAll('.side-nav .nav-label-desktop')].map((element) => element.textContent.trim())
      const mobileLabels = [...document.querySelectorAll('.mobile-bottom-nav .mobile-nav-link small')].map((element) => element.textContent.trim())
      const kicker = [...document.querySelectorAll('.outlook-kicker > span')].map((element) => {
        const rect = element.getBoundingClientRect()
        return { text: element.textContent.trim(), top: Math.round(rect.top), width: rect.width }
      })
      return {
        viewport: window.innerWidth,
        document: document.documentElement.scrollWidth,
        activeNavigation,
        desktopLabels,
        mobileLabels,
        hero: bounds('.outlook-hero'),
        journey: bounds('.journey-panel'),
        kicker,
      }
    })
    expect(layout.document, `horizontal overflow at ${width}px`).toBeLessThanOrEqual(width)
    expect(layout.activeNavigation, `expected one active route at ${width}px`).toBe(1)
    expect(layout.desktopLabels).toEqual(layout.mobileLabels)
    expect(layout.hero.left).toBeGreaterThanOrEqual(0)
    expect(layout.hero.right).toBeLessThanOrEqual(width)
    expect(layout.journey.left).toBeGreaterThanOrEqual(0)
    expect(layout.journey.right).toBeLessThanOrEqual(width)
    const kickerDot = layout.kicker.findIndex((item) => item.width <= 8)
    expect(kickerDot).toBeGreaterThan(0)
    expect(Math.abs(layout.kicker[kickerDot].top - layout.kicker[kickerDot - 1].top)).toBeLessThan(6)
  }

  const routes = ['/dashboard', '/plan/assumptions', '/loans', '/analytics/annual', '/forecast/monthly']
  await page.setViewportSize({ width: 390, height: 844 })
  for (const route of routes) {
    const link = page.locator(`.mobile-bottom-nav a[href="${route}"]`)
    await link.click()
    await expect(page).toHaveURL(new RegExp(`${route.replaceAll('/', '\\/')}$`))
    await expect(link).toHaveAttribute('aria-current', 'page')
    await expect(page.locator('.mobile-bottom-nav .active')).toHaveCount(1)
  }

  await page.setViewportSize({ width: 1280, height: 900 })
  for (const route of routes) {
    const link = page.locator(`.side-nav a[href="${route}"]`)
    await link.click()
    await expect(page).toHaveURL(new RegExp(`${route.replaceAll('/', '\\/')}$`))
    await expect(link).toHaveAttribute('aria-current', 'page')
    await expect(page.locator('.side-nav .active')).toHaveCount(1)
  }
})

test('shows both savings and remaining debt when a journey-chart year is tapped', async ({ dashboardPage }) => {
  const { page } = dashboardPage
  await page.setViewportSize({ width: 390, height: 844 })
  await dashboardPage.open()
  const year = page.locator('.trend-year-selector button').nth(1)
  await year.click()
  await expect(year).toHaveAttribute('aria-pressed', 'true')
  await expect(page.locator('.trend-tooltip')).toContainText('Tiết kiệm:')
  await expect(page.locator('.trend-tooltip')).toContainText('Dư nợ:')
  await page.locator('.trend-point').nth(2).focus()
  await expect(page.locator('.trend-tooltip')).toContainText('2028')
})
