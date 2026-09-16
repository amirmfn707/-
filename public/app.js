/**
 * دستیار صوتی و زمان‌بندی ویرا (Vira Voice Assistant PWA)
 * JavaScript Core Engine: Speech Recognition, GapGPT AI, Jalali Calendar,
 * Apple Calendar (.ics) Export, LocalStorage Database, Web Audio Alarms.
 */

// ==========================================
// 1. JALALI CALENDAR CONVERTER ENGINE
// ==========================================
const PersianCalendar = {
  gregorianToJalali(gy, gm, gd) {
    const g_d_m = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
    let gy2 = (gm > 2) ? (gy + 1) : gy;
    let days = 355666 + (365 * gy) + Math.floor((gy2 + 3) / 4) - Math.floor((gy2 + 99) / 100) + Math.floor((gy2 + 399) / 400) + gd + g_d_m[gm - 1];
    let jy = -1595 + (33 * Math.floor(days / 12053));
    days %= 12053;
    jy += 4 * Math.floor(days / 1461);
    days %= 1461;
    if (days > 365) {
      jy += Math.floor((days - 1) / 365);
      days = (days - 1) % 365;
    }
    let jm, jd;
    if (days < 186) {
      jm = 1 + Math.floor(days / 31);
      jd = 1 + (days % 31);
    } else {
      jm = 7 + Math.floor((days - 186) / 30);
      jd = 1 + ((days - 186) % 30);
    }
    return { jy, jm, jd };
  },

  jalaliToGregorian(jy, jm, jd) {
    let gy = jy + 621;
    let days;
    if (jm <= 6) {
      days = (jm - 1) * 31 + jd;
    } else {
      days = 186 + (jm - 7) * 30 + jd;
    }
    // Calculate approximate Gregorian date
    let date = new Date(gy, 2, 21); // Farvardin 1 is usually around March 21
    date.setDate(date.getDate() + days - 1);
    return date;
  },

  monthNames: [
    "فروردین", "اردیبهشت", "خرداد",
    "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر",
    "دی", "بهمن", "اسفند"
  ],

  dayNames: [
    "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه"
  ],

  toPersianDigits(str) {
    if (str === null || str === undefined) return "";
    const pDigits = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
    return str.toString().replace(/[0-9]/g, (w) => pDigits[+w]);
  },

  formatPersianDate(date) {
    const { jy, jm, jd } = this.gregorianToJalali(date.getFullYear(), date.getMonth() + 1, date.getDate());
    const dayName = this.dayNames[date.getDay()];
    const mName = this.monthNames[jm - 1];
    return `${dayName} ${this.toPersianDigits(jd)} ${mName} ${this.toPersianDigits(jy)}`;
  },

  formatPersianDateTime(date) {
    const dateStr = this.formatPersianDate(date);
    const h = String(date.getHours()).padStart(2, '0');
    const m = String(date.getMinutes()).padStart(2, '0');
    return `${dateStr} ساعت ${this.toPersianDigits(h)}:${this.toPersianDigits(m)}`;
  },

  formatPersianTimeOnly(date) {
    const h = String(date.getHours()).padStart(2, '0');
    const m = String(date.getMinutes()).padStart(2, '0');
    return `${this.toPersianDigits(h)}:${this.toPersianDigits(m)}`;
  }
};

// ==========================================
// 2. STATE & STORAGE MANAGEMENT
// ==========================================
const Storage = {
  KEY_SCHEDULES: 'vira_schedules_list_v1',
  KEY_SETTINGS: 'vira_app_settings_v1',

  getSettings() {
    try {
      const data = localStorage.getItem(this.KEY_SETTINGS);
      return data ? JSON.parse(data) : {
        apiKey: '',
        aiModel: 'gpt-4o-mini',
        defaultAlarmMinutes: 15
      };
    } catch {
      return { apiKey: '', aiModel: 'gpt-4o-mini', defaultAlarmMinutes: 15 };
    }
  },

  saveSettings(settings) {
    localStorage.setItem(this.KEY_SETTINGS, JSON.stringify(settings));
  },

  getSchedules() {
    try {
      const data = localStorage.getItem(this.KEY_SCHEDULES);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  },

  saveSchedules(list) {
    localStorage.setItem(this.KEY_SCHEDULES, JSON.stringify(list));
  },

  addSchedule(item) {
    const list = this.getSchedules();
    item.id = Date.now() + Math.floor(Math.random() * 1000);
    list.unshift(item);
    this.saveSchedules(list);
    return item;
  },

  updateSchedule(id, updates) {
    const list = this.getSchedules();
    const idx = list.findIndex(x => x.id === id);
    if (idx !== -1) {
      list[idx] = { ...list[idx], ...updates };
      this.saveSchedules(list);
    }
  },

  deleteSchedule(id) {
    const list = this.getSchedules().filter(x => x.id !== id);
    this.saveSchedules(list);
  }
};

// ==========================================
// 3. GAPGPT API SERVICE (MULTI-EVENT EXTRACTOR)
// ==========================================
const GapGptService = {
  async parseVoiceText(transcript, settings) {
    const apiKey = (settings.apiKey || '').trim();
    if (!apiKey) {
      throw new Error("لطفاً ابتدا کلید API خود را در بخش تنظیمات وارد نمایید.");
    }

    const now = new Date();
    const nowIso = now.toISOString();
    const persianDesc = PersianCalendar.formatPersianDateTime(now);

    const systemPrompt = `تو یک دستیار صوتی هوشمند، حرفه‌ای و دقیق برای زمان‌بندی، ثبت یادآورها و مدیریت قرارهای کاری به زبان فارسی هستی.
کاربر ممکن است یک، دو یا چند قرار کاری، جلسه یا یادآوری مختلف را در یک پیام صوتی بیان کند.
زمان جاری سیستم: ${nowIso}
تاریخ و روز جاری به شمسی: ${persianDesc}

وظیفه تو این است که کل پیام کاربر را تحلیل کرده و تمام برنامه‌ها، جلسات و یادآوری‌های ذکر شده را استخراج کنی.
قوانین حیاتی:
۱. اگر کاربر دو یا چند کار را در یک پیام گفت (مثلاً: «فردا ساعت ۱۰ جلسه با احمدی دارم و ساعت ۴ بعدازظهر هم یادآوری پرداخت قسط» یا «ساعت ۲ با علی قرار دارم و ساعت ۵ برم دکتر»)، حتماً به ازای هر کدام یک شیء جداگانه در آرایه JSON بساز.
۲. خروجی باید فقط و فقط یک آرایه JSON خام (حتی اگر فقط ۱ برنامه باشد) بدون هیچ متن، توضیح یا بلوک مارک‌داون باشد.

ساختار آرایه JSON:
[
  {
    "title": "عنوان کوتاه و صریح برنامه به فارسی",
    "type": "meeting" یا "reminder" یا "task" یا "event",
    "isoDateTime": "تاریخ و ساعت به فرمت ISO مانند 2026-09-17T10:00:00 (اگر زمان مشخص است، وگرنه null)",
    "durationMinutes": 30,
    "notes": "توضیحات و جزئیات بیشتر در صورت وجود، وگرنه null",
    "reminderMinutesBefore": 15
  }
]

قوانین زمانی:
- "فردا" یعنی ۱ روز بعد از زمان جاری. "پس‌فردا" یعنی ۲ روز بعد.
- اگر فقط ساعت گفته شده باشد (مثلاً ساعت ۴ بعدازظهر)، نسبت به زمان کنونی برای امروز یا فردا بگذار.
- تعیین نوع: جلسه یا قرار کاری -> "meeting"، هشدار و یادآوری -> "reminder"، کار انجام‌دادنی -> "task"، رویداد -> "event".`;

    const response = await fetch("https://api.gapgpt.app/v1/chat/completions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${apiKey}`
      },
      body: JSON.stringify({
        model: settings.aiModel || "gpt-4o-mini",
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: transcript }
        ],
        temperature: 0.2
      })
    });

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(`خطای سرور هوش مصنوعی: ${response.status} - ${errText}`);
    }

    const data = await response.json();
    const rawContent = data.choices?.[0]?.message?.content || "";
    
    // Clean markdown wrappers
    let cleanJson = rawContent
      .replace(/```json/g, '')
      .replace(/```/g, '')
      .trim();

    try {
      let parsed = JSON.parse(cleanJson);
      if (!Array.isArray(parsed)) {
        if (parsed.items || parsed.schedules) {
          parsed = parsed.items || parsed.schedules;
        } else {
          parsed = [parsed];
        }
      }
      return parsed;
    } catch (e) {
      console.warn("Could not parse JSON array directly, fallback:", e);
      return [{
        title: transcript,
        type: "task",
        isoDateTime: null,
        durationMinutes: 30,
        notes: null,
        reminderMinutesBefore: settings.defaultAlarmMinutes || 15
      }];
    }
  }
};

// ==========================================
// 4. APPLE CALENDAR (.ICS) EXPORTER FOR IPHONE
// ==========================================
const AppleCalendarExporter = {
  /**
   * Generates a standard iCalendar (.ics) file.
   * When opened in iOS Safari, iOS automatically opens native Calendar app
   * with the "Add Event" dialog pre-filled, including alert/alarm!
   */
  exportToIcs(item) {
    if (!item.isoDateTime) {
      showToast("این برنامه تاریخ و ساعت مشخصی برای تقویم ندارد.");
      return;
    }

    const startDate = new Date(item.isoDateTime);
    const durationMin = item.durationMinutes || 30;
    const endDate = new Date(startDate.getTime() + durationMin * 60000);

    const pad = (n) => String(n).padStart(2, '0');
    const formatIcsDate = (d) => {
      return `${d.getUTCFullYear()}${pad(d.getUTCMonth() + 1)}${pad(d.getUTCDate())}T${pad(d.getUTCHours())}${pad(d.getUTCMinutes())}00Z`;
    };

    const startFormatted = formatIcsDate(startDate);
    const endFormatted = formatIcsDate(endDate);
    const nowFormatted = formatIcsDate(new Date());
    const uid = `vira-${item.id}-${Date.now()}@vira.assistant`;

    const reminderMin = item.reminderMinutesBefore || 15;

    const icsContent = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'PRODID:-//Vira Assistant//Persian Voice Schedule//FA',
      'CALSCALE:GREGORIAN',
      'METHOD:PUBLISH',
      'BEGIN:VEVENT',
      `UID:${uid}`,
      `DTSTAMP:${nowFormatted}`,
      `DTSTART:${startFormatted}`,
      `DTEND:${endFormatted}`,
      `SUMMARY:${item.title}`,
      `DESCRIPTION:${(item.notes || 'ثبت‌شده با دستیار هوشمند ویرا').replace(/\n/g, '\\n')}`,
      'STATUS:CONFIRMED',
      'BEGIN:VALARM',
      `TRIGGER:-PT${reminderMin}M`,
      'ACTION:DISPLAY',
      `DESCRIPTION:یادآوری: ${item.title}`,
      'END:VALARM',
      'END:VEVENT',
      'END:VCALENDAR'
    ].join('\r\n');

    // Create Blob and trigger download for Safari iOS
    const blob = new Blob([icsContent], { type: 'text/calendar;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${item.title.replace(/[\/\\:*?"<>|]/g, '_')}.ics`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    showToast("فایل تقویم آماده شد. در آیفون لمس کنید تا در تقویم رسمی ثبت شود.");
  }
};

// ==========================================
// 5. AUDIO ALARM SYNTHESIZER
// ==========================================
const SoundPlayer = {
  audioCtx: null,
  alarmOscillator: null,
  alarmInterval: null,

  initAudio() {
    if (!this.audioCtx) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (AudioContext) {
        this.audioCtx = new AudioContext();
      }
    }
  },

  playChime() {
    try {
      this.initAudio();
      if (!this.audioCtx) return;
      if (this.audioCtx.state === 'suspended') {
        this.audioCtx.resume();
      }
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(587.33, this.audioCtx.currentTime); // D5
      osc.frequency.exponentialRampToValueAtTime(880, this.audioCtx.currentTime + 0.15); // A5
      gain.gain.setValueAtTime(0.3, this.audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, this.audioCtx.currentTime + 0.4);
      osc.connect(gain);
      gain.connect(this.audioCtx.destination);
      osc.start();
      osc.stop(this.audioCtx.currentTime + 0.4);
    } catch (e) {
      console.warn("Audio chime error", e);
    }
  },

  startAlarmSound() {
    try {
      this.initAudio();
      if (!this.audioCtx) return;
      if (this.audioCtx.state === 'suspended') {
        this.audioCtx.resume();
      }
      let beep = true;
      const playBeep = () => {
        if (!beep) return;
        const osc = this.audioCtx.createOscillator();
        const gain = this.audioCtx.createGain();
        osc.type = 'square';
        osc.frequency.setValueAtTime(880, this.audioCtx.currentTime);
        gain.gain.setValueAtTime(0.35, this.audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, this.audioCtx.currentTime + 0.25);
        osc.connect(gain);
        gain.connect(this.audioCtx.destination);
        osc.start();
        osc.stop(this.audioCtx.currentTime + 0.25);
      };
      playBeep();
      this.alarmInterval = setInterval(playBeep, 600);
    } catch (e) {
      console.warn("Alarm audio error", e);
    }
  },

  stopAlarmSound() {
    if (this.alarmInterval) {
      clearInterval(this.alarmInterval);
      this.alarmInterval = null;
    }
  }
};

// ==========================================
// 6. APPLICATION CONTROLLER
// ==========================================
let currentViewMode = 'cards'; // 'cards' | 'calendar' | 'table'
let currentFilter = 'all';     // 'all' | 'today' | 'future' | 'completed'
let selectedCalDate = new Date();
let speechRecognition = null;
let isRecording = false;

// Initialize on DOM Ready
document.addEventListener('DOMContentLoaded', () => {
  initServiceWorker();
  initHeaderDate();
  initSpeechRecognition();
  initAlarmScheduler();
  renderSchedules();
  setupEventListeners();
  checkIosBanner();
});

function initServiceWorker() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js')
      .then(() => console.log("PWA ServiceWorker registered"))
      .catch((err) => console.warn("ServiceWorker registration failed", err));
  }
}

function initHeaderDate() {
  const today = new Date();
  const dateStr = PersianCalendar.formatPersianDate(today);
  const badge = document.getElementById('currentDateBadge');
  if (badge) badge.textContent = dateStr;
}

function checkIosBanner() {
  const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
  const isStandalone = window.navigator.standalone || window.matchMedia('(display-mode: standalone)').matches;
  const banner = document.getElementById('iosInstallBanner');
  
  if (isIos && !isStandalone && banner) {
    banner.style.display = 'flex';
  } else if (banner) {
    banner.style.display = 'none';
  }
}

// Speech Recognition setup (Persian fa-IR)
function initSpeechRecognition() {
  const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (SpeechRec) {
    speechRecognition = new SpeechRec();
    speechRecognition.lang = 'fa-IR';
    speechRecognition.continuous = false;
    speechRecognition.interimResults = false;

    speechRecognition.onstart = () => {
      isRecording = true;
      updateMicButtonState(true);
      showAiStatus(true, "در حال گوش دادن به زبان فارسی…", "لطفاً برنامه یا جلسات کاری خود را بگویید");
      SoundPlayer.playChime();
    };

    speechRecognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      processVoiceTranscript(transcript);
    };

    speechRecognition.onerror = (event) => {
      console.warn("Speech recognition error:", event.error);
      isRecording = false;
      updateMicButtonState(false);
      showAiStatus(false);
      if (event.error === 'not-allowed') {
        showToast("دسترسی به میکروفون داده نشده است.");
      } else {
        showToast("متوجه صدا نشدم، لطفاً مجدداً تلاش کنید.");
      }
    };

    speechRecognition.onend = () => {
      isRecording = false;
      updateMicButtonState(false);
    };
  }
}

function toggleVoiceRecording() {
  if (!speechRecognition) {
    showToast("مرورگر شما از تشخیص گفتار پشتیبانی نمی‌کند، لطفاً از کادر متنی استفاده کنید.");
    return;
  }

  if (isRecording) {
    speechRecognition.stop();
  } else {
    try {
      speechRecognition.start();
    } catch (e) {
      console.warn("Could not start speech recognition", e);
    }
  }
}

function updateMicButtonState(recording) {
  const btn = document.getElementById('btnMasterMic');
  if (!btn) return;
  if (recording) {
    btn.classList.add('recording');
  } else {
    btn.classList.remove('recording');
  }
}

async function processVoiceTranscript(transcript) {
  if (!transcript || !transcript.trim()) return;

  const settings = Storage.getSettings();
  if (!settings.apiKey) {
    showAiStatus(false);
    showToast("لطفاً ابتدا کلید API خود را در بخش تنظیمات وارد نمایید.");
    openSettingsModal();
    return;
  }

  showAiStatus(true, "در حال تحلیل با هوش مصنوعی…", `«${transcript}»`);

  try {
    const items = await GapGptService.parseVoiceText(transcript, settings);
    showAiStatus(false);

    let count = 0;
    for (const item of items) {
      Storage.addSchedule({
        title: item.title || transcript,
        type: item.type || 'task',
        isoDateTime: item.isoDateTime || null,
        durationMinutes: item.durationMinutes || 30,
        notes: item.notes || null,
        reminderMinutesBefore: item.reminderMinutesBefore || settings.defaultAlarmMinutes || 15,
        isAlarmEnabled: !!item.isoDateTime,
        isCompleted: false,
        originalVoiceTranscript: transcript
      });
      count++;
    }

    SoundPlayer.playChime();
    renderSchedules();

    if (count === 1) {
      const single = items[0];
      const timeStr = single.isoDateTime 
        ? PersianCalendar.formatPersianDateTime(new Date(single.isoDateTime))
        : "بدون زمان";
      showToast(`ثبت شد: ${single.title} (${timeStr})`);
    } else {
      showToast(`✅ ${PersianCalendar.toPersianDigits(count)} برنامه کاری با موفقیت ثبت شد.`);
    }
  } catch (err) {
    showAiStatus(false);
    showToast(`خطا: ${err.message}`);
  }
}

function showAiStatus(show, title = "", desc = "") {
  const card = document.getElementById('aiStatusCard');
  if (!card) return;
  if (show) {
    card.style.display = 'flex';
    document.getElementById('aiStatusTitle').textContent = title;
    document.getElementById('aiStatusDesc').textContent = desc;
  } else {
    card.style.display = 'none';
  }
}

// ==========================================
// 7. RENDERING & UI LOGIC
// ==========================================
function renderSchedules() {
  const container = document.getElementById('contentArea');
  if (!container) return;

  const allItems = Storage.getSchedules();

  // Filter based on selected filter
  let filtered = allItems;
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const todayEnd = todayStart + (24 * 60 * 60 * 1000);

  if (currentFilter === 'today') {
    filtered = allItems.filter(item => {
      if (!item.isoDateTime) return false;
      const t = new Date(item.isoDateTime).getTime();
      return t >= todayStart && t < todayEnd;
    });
  } else if (currentFilter === 'future') {
    filtered = allItems.filter(item => {
      if (!item.isoDateTime) return false;
      return new Date(item.isoDateTime).getTime() >= now.getTime();
    });
  } else if (currentFilter === 'completed') {
    filtered = allItems.filter(item => item.isCompleted);
  }

  if (currentViewMode === 'cards') {
    renderCardsView(container, filtered);
  } else if (currentViewMode === 'calendar') {
    renderCalendarView(container, allItems);
  } else if (currentViewMode === 'table') {
    renderTableView(container, filtered);
  }
}

function renderCardsView(container, items) {
  if (items.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">📅</div>
        <div class="empty-title">برنامه‌ای برای نمایش وجود ندارد</div>
        <div class="empty-desc">برای افزودن، دکمه میکروفون پایین صفحه را لمس کنید یا از کادر متنی بنویسید.</div>
      </div>
    `;
    return;
  }

  const typeLabels = {
    meeting: { label: 'جلسه کاری', cls: 'meeting', icon: '👥' },
    reminder: { label: 'یادآوری', cls: 'reminder', icon: '⏰' },
    task: { label: 'کار شخصی', cls: 'task', icon: '📝' },
    event: { label: 'رویداد', cls: 'event', icon: '📌' }
  };

  const html = items.map(item => {
    const typeInfo = typeLabels[item.type] || typeLabels.task;
    const timeFormatted = item.isoDateTime 
      ? PersianCalendar.formatPersianDateTime(new Date(item.isoDateTime))
      : "بدون زمان مشخص";

    return `
      <div class="schedule-card ${item.isCompleted ? 'completed' : ''}" data-type="${item.type || 'task'}">
        <div class="card-header-row">
          <span class="card-type-tag ${typeInfo.cls}">
            ${typeInfo.icon} ${typeInfo.label}
          </span>
          <div class="card-actions-top">
            <button class="btn-card-action ${item.isAlarmEnabled ? 'alarm-active' : ''}" title="هشدار و آلارم" onclick="toggleItemAlarm(${item.id})">
              ${item.isAlarmEnabled ? '🔔' : '🔕'}
            </button>
            <button class="btn-card-action" title="تکمیل کار" onclick="toggleItemCompleted(${item.id})">
              ${item.isCompleted ? '☑️' : '◻️'}
            </button>
            <button class="btn-card-action" title="حذف" onclick="deleteItem(${item.id})">
              🗑️
            </button>
          </div>
        </div>

        <div class="card-body">
          <div class="card-title">${escapeHtml(item.title)}</div>
          ${item.notes ? `<div class="card-notes">${escapeHtml(item.notes)}</div>` : ''}
        </div>

        <div class="card-footer-row">
          <div class="time-info">
            <span>🕒 ${timeFormatted}</span>
          </div>
          ${item.isoDateTime ? `
            <button class="btn-add-apple-cal" onclick="exportAppleCal(${item.id})">
              <span>🍏 افزودن به تقویم آیفون</span>
            </button>
          ` : ''}
        </div>
      </div>
    `;
  }).join('');

  container.innerHTML = html;
}

function renderCalendarView(container, allItems) {
  const today = new Date();
  const { jy: curJy, jm: curJm } = PersianCalendar.gregorianToJalali(selectedCalDate.getFullYear(), selectedCalDate.getMonth() + 1, selectedCalDate.getDate());
  const monthTitle = `${PersianCalendar.monthNames[curJm - 1]} ${PersianCalendar.toPersianDigits(curJy)}`;

  // Determine days in this Jalali month (1-6 have 31 days, 7-11 have 30 days, 12 has 29 or 30)
  const daysInMonth = (curJm <= 6) ? 31 : ((curJm <= 11) ? 30 : 29);

  // Map dates with events
  const dayEventsMap = {};
  allItems.forEach(item => {
    if (item.isoDateTime) {
      const d = new Date(item.isoDateTime);
      const j = PersianCalendar.gregorianToJalali(d.getFullYear(), d.getMonth() + 1, d.getDate());
      if (j.jy === curJy && j.jm === curJm) {
        dayEventsMap[j.jd] = (dayEventsMap[j.jd] || 0) + 1;
      }
    }
  });

  const { jy: tJy, jm: tJm, jd: tJd } = PersianCalendar.gregorianToJalali(today.getFullYear(), today.getMonth() + 1, today.getDate());

  let cellsHtml = '';
  for (let d = 1; d <= daysInMonth; d++) {
    const isToday = (curJy === tJy && curJm === tJm && d === tJd);
    const hasEvents = !!dayEventsMap[d];
    cellsHtml += `
      <div class="cal-cell ${isToday ? 'today' : ''} ${hasEvents ? 'has-events' : ''}" onclick="selectCalDay(${curJy}, ${curJm}, ${d})">
        ${PersianCalendar.toPersianDigits(d)}
      </div>
    `;
  }

  container.innerHTML = `
    <div class="calendar-view-card">
      <div class="cal-header">
        <div class="cal-month-title">📅 ${monthTitle}</div>
        <div class="cal-nav-btns">
          <button class="btn-icon" onclick="changeCalMonth(-1)">‹</button>
          <button class="btn-icon" onclick="changeCalMonth(1)">›</button>
        </div>
      </div>
      <div class="cal-days-header">
        <div>ش</div><div>ی</div><div>د</div><div>س</div><div>چ</div><div>پ</div><div>ج</div>
      </div>
      <div class="cal-grid">
        ${cellsHtml}
      </div>
    </div>
    <div id="calDayEventsContainer"></div>
  `;

  // Preload today's events under calendar
  selectCalDay(curJy, curJm, tJd);
}

function selectCalDay(jy, jm, jd) {
  const container = document.getElementById('calDayEventsContainer');
  if (!container) return;

  const allItems = Storage.getSchedules();
  const dayItems = allItems.filter(item => {
    if (!item.isoDateTime) return false;
    const d = new Date(item.isoDateTime);
    const j = PersianCalendar.gregorianToJalali(d.getFullYear(), d.getMonth() + 1, d.getDate());
    return j.jy === jy && j.jm === jm && j.jd === jd;
  });

  const dateStr = `${PersianCalendar.toPersianDigits(jd)} ${PersianCalendar.monthNames[jm - 1]} ${PersianCalendar.toPersianDigits(jy)}`;

  if (dayItems.length === 0) {
    container.innerHTML = `
      <div class="empty-state" style="padding: 24px 12px;">
        <div class="empty-desc">هیچ برنامه‌ای برای ${dateStr} ثبت نشده است.</div>
      </div>
    `;
  } else {
    container.innerHTML = `
      <div style="font-weight: 700; color: #C7D2FE; margin-bottom: 12px; font-size: 0.88rem;">
        برنامه‌های ${dateStr} (${PersianCalendar.toPersianDigits(dayItems.length)} مورد):
      </div>
    `;
    const tempDiv = document.createElement('div');
    renderCardsView(tempDiv, dayItems);
    container.appendChild(tempDiv);
  }
}

function changeCalMonth(delta) {
  selectedCalDate = new Date(selectedCalDate.getFullYear(), selectedCalDate.getMonth() + delta, 1);
  renderSchedules();
}

function renderTableView(container, items) {
  if (items.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-title">جدول زمان‌بندی خالی است</div>
      </div>
    `;
    return;
  }

  // Sort chronologically
  const sorted = [...items].sort((a, b) => {
    const tA = a.isoDateTime ? new Date(a.isoDateTime).getTime() : 0;
    const tB = b.isoDateTime ? new Date(b.isoDateTime).getTime() : 0;
    return tA - tB;
  });

  const rows = sorted.map(item => {
    const timeStr = item.isoDateTime
      ? PersianCalendar.formatPersianDateTime(new Date(item.isoDateTime))
      : "بدون زمان";
    return `
      <tr style="border-bottom: 1px solid rgba(255,255,255,0.06);">
        <td style="padding: 10px 8px; font-size: 0.82rem; font-weight: 700;">${escapeHtml(item.title)}</td>
        <td style="padding: 10px 8px; font-size: 0.78rem; color: #A5B4FC;">${timeStr}</td>
        <td style="padding: 10px 8px; text-align: center;">
          <button class="btn-card-action" onclick="toggleItemCompleted(${item.id})">
            ${item.isCompleted ? '✅' : '⏳'}
          </button>
        </td>
      </tr>
    `;
  }).join('');

  container.innerHTML = `
    <div style="background: var(--bg-card); border-radius: var(--radius-md); overflow: hidden; border: 1px solid var(--border-subtle);">
      <table style="width: 100%; border-collapse: collapse; text-align: right;">
        <thead>
          <tr style="background: rgba(255,255,255,0.04); color: var(--text-dim); font-size: 0.75rem;">
            <th style="padding: 10px 8px;">عنوان</th>
            <th style="padding: 10px 8px;">زمان</th>
            <th style="padding: 10px 8px; text-align: center;">وضعیت</th>
          </tr>
        </thead>
        <tbody>
          ${rows}
        </tbody>
      </table>
    </div>
  `;
}

// ==========================================
// 8. ACTIONS & HANDLERS
// ==========================================
function toggleItemCompleted(id) {
  const item = Storage.getSchedules().find(x => x.id === id);
  if (item) {
    Storage.updateSchedule(id, { isCompleted: !item.isCompleted });
    renderSchedules();
  }
}

function toggleItemAlarm(id) {
  const item = Storage.getSchedules().find(x => x.id === id);
  if (item) {
    const newState = !item.isAlarmEnabled;
    Storage.updateSchedule(id, { isAlarmEnabled: newState });
    renderSchedules();
    showToast(newState ? "⏰ هشدار فعال شد" : "🔕 هشدار غیرفعال شد");
  }
}

function deleteItem(id) {
  if (confirm("آیا از حذف این برنامه کاری اطمینان دارید؟")) {
    Storage.deleteSchedule(id);
    renderSchedules();
    showToast("برنامه حذف شد.");
  }
}

function exportAppleCal(id) {
  const item = Storage.getSchedules().find(x => x.id === id);
  if (item) {
    AppleCalendarExporter.exportToIcs(item);
  }
}

// In-Browser Alarm Scheduler Loop
function initAlarmScheduler() {
  setInterval(() => {
    const now = Date.now();
    const items = Storage.getSchedules();

    items.forEach(item => {
      if (item.isAlarmEnabled && item.isoDateTime && !item.isCompleted && !item._alarmTriggered) {
        const eventTime = new Date(item.isoDateTime).getTime();
        const advanceMillis = (item.reminderMinutesBefore || 15) * 60 * 1000;
        const triggerTime = eventTime - advanceMillis;

        // If trigger time reached within past 2 minutes
        if (now >= triggerTime && now <= eventTime + 60000) {
          item._alarmTriggered = true;
          triggerAlarmAlert(item);
        }
      }
    });
  }, 10000);
}

function triggerAlarmAlert(item) {
  SoundPlayer.startAlarmSound();
  if (navigator.vibrate) {
    navigator.vibrate([400, 200, 400, 200, 800]);
  }

  const overlay = document.getElementById('alarmOverlay');
  if (overlay) {
    document.getElementById('alarmOverlayTitle').textContent = item.title;
    document.getElementById('alarmOverlayDesc').textContent = item.notes || 'موعد جلسه یا یادآوری کاری شما فرا رسیده است.';
    overlay.classList.add('active');
  }

  // Web Notification if allowed
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(`⏰ یادآوری کاری: ${item.title}`, {
      body: item.notes || 'موعد برنامه فرا رسیده است.',
      icon: 'icons/icon-192.png'
    });
  }
}

function dismissAlarm() {
  SoundPlayer.stopAlarmSound();
  const overlay = document.getElementById('alarmOverlay');
  if (overlay) {
    overlay.classList.remove('active');
  }
}

// Modal Management
function openAddModal() {
  const modal = document.getElementById('addModal');
  if (modal) modal.classList.add('open');
}

function closeAddModal() {
  const modal = document.getElementById('addModal');
  if (modal) modal.classList.remove('open');
}

function saveManualItem(e) {
  e.preventDefault();
  const title = document.getElementById('addTitle').value.trim();
  const type = document.getElementById('addType').value;
  const dateVal = document.getElementById('addDate').value;
  const timeVal = document.getElementById('addTime').value;
  const notes = document.getElementById('addNotes').value.trim();
  const reminderMin = parseInt(document.getElementById('addReminder').value, 10) || 15;

  if (!title) {
    showToast("لطفاً عنوان برنامه را بنویسید.");
    return;
  }

  let isoDateTime = null;
  if (dateVal && timeVal) {
    isoDateTime = `${dateVal}T${timeVal}:00`;
  }

  Storage.addSchedule({
    title,
    type,
    isoDateTime,
    durationMinutes: 30,
    notes: notes || null,
    reminderMinutesBefore: reminderMin,
    isAlarmEnabled: !!isoDateTime,
    isCompleted: false
  });

  closeAddModal();
  renderSchedules();
  showToast("برنامه با موفقیت اضافه شد.");

  // Clear form
  document.getElementById('manualAddForm').reset();
}

function openSettingsModal() {
  const settings = Storage.getSettings();
  document.getElementById('settingApiKey').value = settings.apiKey || '';
  document.getElementById('settingModel').value = settings.aiModel || 'gpt-4o-mini';
  document.getElementById('settingDefaultAlarm').value = settings.defaultAlarmMinutes || 15;

  const modal = document.getElementById('settingsModal');
  if (modal) modal.classList.add('open');
}

function closeSettingsModal() {
  const modal = document.getElementById('settingsModal');
  if (modal) modal.classList.remove('open');
}

function saveSettingsForm(e) {
  e.preventDefault();
  const apiKey = document.getElementById('settingApiKey').value.trim();
  const aiModel = document.getElementById('settingModel').value;
  const defaultAlarmMinutes = parseInt(document.getElementById('settingDefaultAlarm').value, 10) || 15;

  Storage.saveSettings({ apiKey, aiModel, defaultAlarmMinutes });
  closeSettingsModal();
  showToast("تنظیمات با موفقیت ذخیره شد.");
}

function openIosGuideModal() {
  const modal = document.getElementById('iosGuideModal');
  if (modal) modal.classList.add('open');
}

function closeIosGuideModal() {
  const modal = document.getElementById('iosGuideModal');
  if (modal) modal.classList.remove('open');
}

// Toast
function showToast(msg) {
  const toast = document.getElementById('toast');
  if (!toast) return;
  toast.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 3500);
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>'"]/g, 
    tag => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      "'": '&#39;',
      '"': '&quot;'
    }[tag] || tag)
  );
}

// Event Listeners setup
function setupEventListeners() {
  // Mic Floating button
  document.getElementById('btnMasterMic')?.addEventListener('click', () => {
    toggleVoiceRecording();
  });

  // Dock Text submit
  document.getElementById('btnDockSend')?.addEventListener('click', () => {
    const input = document.getElementById('dockTextInput');
    const val = input.value.trim();
    if (val) {
      processVoiceTranscript(val);
      input.value = '';
    }
  });

  document.getElementById('dockTextInput')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      const val = e.target.value.trim();
      if (val) {
        processVoiceTranscript(val);
        e.target.value = '';
      }
    }
  });

  // View mode switcher buttons
  document.querySelectorAll('.view-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      document.querySelectorAll('.view-btn').forEach(b => b.classList.remove('active'));
      const target = e.currentTarget;
      target.classList.add('active');
      currentViewMode = target.getAttribute('data-mode');
      renderSchedules();
    });
  });

  // Filter chips
  document.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', (e) => {
      document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
      const target = e.currentTarget;
      target.classList.add('active');
      currentFilter = target.getAttribute('data-filter');
      renderSchedules();
    });
  });

  // Forms
  document.getElementById('manualAddForm')?.addEventListener('submit', saveManualItem);
  document.getElementById('settingsForm')?.addEventListener('submit', saveSettingsForm);

  // Request notification permission if available
  if ('Notification' in window && Notification.permission === 'default') {
    document.body.addEventListener('click', () => {
      Notification.requestPermission().catch(() => {});
    }, { once: true });
  }
}
