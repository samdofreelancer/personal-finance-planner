<template>
  <section class="panel assumptions-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">ĐẦU VÀO</p>
        <h2>Giả định của bạn</h2>
      </div>
      <button class="button text-button" @click="$emit('reset')">Khôi phục mặc định</button>
    </div>
    <form @submit.prevent="$emit('save', dto)">
      <div class="input-groups">
        <fieldset class="input-group">
          <legend><span class="legend-icon income-icon">↗</span> Thu nhập</legend>
          <div class="field">
            <span>Lương tháng cơ sở năm 2026</span>
            <span class="input-wrap"><input v-model.number="dto.baseSalary" type="number" min="0" step="1" required><span>₫ / tháng</span></span>
          </div>
          <div class="field">
            <span>Hệ số lương tháng 1</span>
            <span class="input-wrap input-wrap-short"><input v-model.number="dto.januaryMultiplier" type="number" min="0" max="20" step="0.1" required><span>×</span></span>
          </div>
          <p class="field-hint">Các đợt tăng lương tháng 4 được áp dụng cộng dồn.</p>
          <div class="raise-grid" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:0.5rem;padding-top:0.5rem;">
            <div class="field" v-for="(r, y) in dto.raises" :key="y">
              <label style="display:flex;flex-direction:column;gap:0.25rem;">
                <span style="font-size:0.8125rem;">Tăng từ 04/{{ y }}</span>
                <input v-model.number="dto.raises[y]" type="number" min="0" max="1" step="0.01" style="padding:0.375rem;border:1px solid #d1d5db;border-radius:4px;">
                <span style="font-size:0.75rem;color:#6b7280;">×100%</span>
              </label>
            </div>
          </div>
        </fieldset>
      </div>
      <div class="input-groups" style="margin-top:1rem;">
        <fieldset class="input-group">
          <legend><span class="legend-icon" style="color:#059669;">👶</span> Chi phí sinh hoạt &amp; con</legend>
          <div class="field"><span>Sinh hoạt / tháng</span><span class="input-wrap"><input v-model.number="dto.living" type="number" min="0" step="1" required><span>₫</span></span></div>
          <div class="field"><span>Nuôi con đến 12/2026</span><span class="input-wrap"><input v-model.number="dto.childThrough2026" type="number" min="0" step="1" required><span>₫ / tháng</span></span></div>
          <div class="field"><span>Nuôi con Q1/2027</span><span class="input-wrap"><input v-model.number="dto.childQ12027" type="number" min="0" step="1" required><span>₫ / tháng</span></span></div>
          <div class="field"><span>Nuôi con từ 04/2027</span><span class="input-wrap"><input v-model.number="dto.childFromApr2027" type="number" min="0" step="1" required><span>₫ / tháng</span></span></div>
        </fieldset>
      </div>
      <div class="input-groups" style="margin-top:1rem;">
        <fieldset class="input-group">
          <legend><span class="legend-icon" style="color:#dc2626;">🏠</span> Tiền nhà &amp; tiền tiết kiệm</legend>
          <div class="field"><span>Tiền nhà đến 01/2027</span><span class="input-wrap"><input v-model.number="dto.rentThroughJan2027" type="number" min="0" step="1" required><span>₫ / tháng</span></span></div>
          <div class="field"><span>Tiền nhà từ 02/2027</span><span class="input-wrap"><input v-model.number="dto.rentFromFeb2027" type="number" min="0" step="1" required><span>₫ / tháng</span></span></div>
          <div class="field"><span>Tiền tiết kiệm ban đầu</span><span class="input-wrap"><input v-model.number="dto.startingSavings" type="number" min="0" step="1" required><span>₫</span></span></div>
        </fieldset>
      </div>
      <div class="input-groups" style="margin-top:1rem;">
        <fieldset class="input-group">
          <legend><span class="legend-icon" style="color:#2563eb;">🏦</span> Khoản vay &amp; lãi</legend>
          <div class="field"><span>Gốc nợ lần đầu (28/10/2026)</span><span class="input-wrap"><input v-model.number="dto.loanPrincipal" type="number" min="0" step="1" required><span>₫</span></span></div>
          <div class="field"><span>Trả cố định 11,12/2026</span><span class="input-wrap"><input v-model.number="dto.fixedPayment" type="number" min="0" step="1" required><span>₫ / tháng</span></span></div>
          <div class="field"><span>Lãi ngân hàng 30/11/2026</span><span class="input-wrap"><input v-model.number="dto.novInterest" type="number" min="0" step="1" required><span>₫</span></span></div>
          <div class="field"><span>Lãi ngân hàng 28/12/2026</span><span class="input-wrap"><input v-model.number="dto.decInterest" type="number" min="0" step="1" required><span>₫</span></span></div>
          <div class="field"><span>Lãi tất toán 25/01/2027</span><span class="input-wrap"><input v-model.number="dto.payoffInterest" type="number" min="0" step="1" placeholder="Chưa xác nhận"><span>₫</span></span></div>
          <div class="confirm-field"><input type="checkbox" v-model="dto.payoffInterestConfirmed" id="payoffConfirmed"><label for="payoffConfirmed">Đã đối chiếu số lãi này với ngân hàng</label></div>
          <p class="field-hint">Lãi suất 13,2%/năm chỉ để tham khảo, không dùng để ước tính lãi thực tế.</p>
        </fieldset>
      </div>
      <div id="form-error" class="form-error" v-if="error">{{ error }}</div>
      <div class="form-footer">
        <span id="save-status" class="save-status">{{ status }}</span>
        <span class="form-note">Đơn vị tiền tệ: VND</span>
      </div>
    </form>
  </section>
</template>

<script setup>
defineProps(['dto', 'error', 'status'])
</script>