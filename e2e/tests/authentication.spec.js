import { test, expect } from '../fixtures/test.js'

test('owner invites a separate member account into the shared household', async ({ authenticatedPage: page, browser }) => {
  const memberEmail = `e2e-member-${Date.now()}@planner.test`
  await page.getByRole('button', { name: 'Hộ gia đình' }).click()
  await expect(page.getByRole('heading', { name: 'Quản lý lời mời' })).toBeVisible()
  await page.getByRole('button', { name: '+ Tạo lời mời' }).click()

  const invitationInput = page.locator('#invitation-link')
  await expect(invitationInput).toBeVisible()
  const invitationUrl = await invitationInput.inputValue()
  expect(invitationUrl).toContain('?invite=')

  const memberContext = await browser.newContext()
  try {
    const memberPage = await memberContext.newPage()
    await memberPage.goto(invitationUrl)
    await expect(memberPage.getByRole('heading', { name: 'Tham gia household' })).toBeVisible()
    await memberPage.getByLabel('Email').fill(memberEmail)
    await memberPage.getByLabel('Mật khẩu', { exact: true }).fill('e2e-member-password-2026')
    await memberPage.getByLabel('Xác nhận mật khẩu').fill('e2e-member-password-2026')
    await memberPage.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click()

    await expect(memberPage.getByRole('button', { name: 'Đăng xuất' })).toBeVisible()
    await expect(memberPage.getByText(memberEmail)).toBeVisible()

    const memberMutation = await memberPage.evaluate(async () => {
      const current = await fetch('/api/assumptions').then((response) => response.json())
      const csrf = await fetch('/api/csrf').then((response) => response.json())
      const updatedIncomeChanges = current.incomeChanges.map((change, index) => index === 0 ? { ...change, amount: change.amount + 1_000_000 } : change)
      const updateResponse = await fetch('/api/assumptions', {
        method: 'PUT',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrf.token },
        body: JSON.stringify({ ...current, incomeChanges: updatedIncomeChanges }),
      })
      const invitationResponse = await fetch('/api/household/invitations', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrf.token },
        body: JSON.stringify({ validityDays: 7 }),
      })
      return { updateStatus: updateResponse.status, updatedIncomeAmount: updatedIncomeChanges[0].amount, invitationStatus: invitationResponse.status }
    })
    expect(memberMutation.updateStatus, 'Household members can update shared financial assumptions').toBe(200)
    expect(memberMutation.invitationStatus, 'Only the owner can create invitations').toBe(403)
    const ownerAssumptions = await page.evaluate(async () => fetch('/api/assumptions').then((response) => response.json()))
    expect(ownerAssumptions.incomeChanges[0].amount).toBe(memberMutation.updatedIncomeAmount)

    // The new account is a household member, not a household owner.
    await memberPage.getByRole('button', { name: 'Hộ gia đình' }).click()
    await expect(memberPage.getByText('Bạn là thành viên của household này.')).toBeVisible()
    await expect(memberPage.locator('#create-invitation')).toHaveCount(0)
  } finally {
    await memberContext.close()
  }

  // Owner sees the invitation consumed after the member has registered.
  await page.reload()
  await page.getByRole('button', { name: 'Hộ gia đình' }).click()
  await expect(page.getByText('Đã sử dụng').first()).toBeVisible()
})

test('authenticated user can log out and return to the login screen', async ({ authenticatedPage: page }) => {
  await page.getByRole('button', { name: 'Đăng xuất' }).click()
  await expect(page.getByRole('heading', { name: 'Chào mừng trở lại' })).toBeVisible()
  await expect(page.getByLabel('Email')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Chưa có tài khoản? Tạo tài khoản' })).toBeVisible()
})
