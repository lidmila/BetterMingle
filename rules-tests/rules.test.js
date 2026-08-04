/**
 * Testy bezpečnostních pravidel Firestore.
 *
 * Spouští se proti emulátoru:  npm test  (v této složce)
 * Emulátor potřebuje Java 17+; na tomhle stroji je v ~/.gradle/jdks.
 *
 * Smysl je dvojí:
 *  1) ukotvit chování, na kterém stojí mobilní aplikace, aby se zpřísnění
 *     pravidel dalo nasadit s jistotou, že nic nerozbije,
 *  2) pohlídat, že se nevrátí díry, kvůli kterým se pravidla upravovala.
 */

import { readFileSync } from 'node:fs';
import { after, before, describe, it } from 'node:test';
import assert from 'node:assert/strict';

import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  collection,
  addDoc,
} from 'firebase/firestore';

const ORGANIZER = 'uid_organizator';
const MEMBER = 'uid_ucastnik';
const OUTSIDER = 'uid_cizi';
const EVENT = 'akce1';

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'bettermingle',
    firestore: {
      rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });
});

after(async () => {
  await testEnv?.cleanup();
});

/** Výchozí stav: akce s organizátorem a jedním účastníkem. */
async function seed({ hideFinancials = false } = {}) {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'events', EVENT), {
      createdBy: ORGANIZER,
      name: 'Testovaci akce',
      securityEnabled: hideFinancials,
      hideFinancials,
      requireApproval: false,
      inviteCode: 'ABC123',
    });
    for (const uid of [ORGANIZER, MEMBER]) {
      await setDoc(doc(db, 'events', EVENT, 'participants', uid), { userId: uid, role: 'PARTICIPANT' });
    }
    await setDoc(doc(db, 'events', EVENT, 'ratings', MEMBER), { userId: MEMBER, overallRating: 5 });
    await setDoc(doc(db, 'events', EVENT, 'lastSeen', MEMBER), { chat: 1 });
    await addDoc(collection(db, 'events', EVENT, 'activity'), {
      actorId: MEMBER,
      description: 'neco udelal',
      timestamp: Date.now(),
    });
    await setDoc(doc(db, 'users', MEMBER), {
      displayName: 'Ucastnik',
      phone: '+420111222333',
      dietaryPreferences: ['dietary_nuts'],
    });
  });
}

const as = (uid) => testEnv.authenticatedContext(uid).firestore();

describe('akce', () => {
  it('účastník akci přečte, cizí ne', async () => {
    await seed();
    await assertSucceeds(getDoc(doc(as(MEMBER), 'events', EVENT)));
    await assertFails(getDoc(doc(as(OUTSIDER), 'events', EVENT)));
  });

  it('akci smí měnit jen organizátor', async () => {
    await seed();
    await assertSucceeds(setDoc(doc(as(ORGANIZER), 'events', EVENT), { name: 'Nove' }, { merge: true }));
    await assertFails(setDoc(doc(as(MEMBER), 'events', EVENT), { name: 'Cizi' }, { merge: true }));
  });
});

describe('moduly akce', () => {
  it('účastník smí přidávat položky programu', async () => {
    await seed();
    await assertSucceeds(
      addDoc(collection(as(MEMBER), 'events', EVENT, 'schedule'), { title: 'Sraz' })
    );
  });

  it('cizí do modulů nevidí ani nezapíše', async () => {
    await seed();
    await assertFails(getDoc(doc(as(OUTSIDER), 'events', EVENT, 'ratings', MEMBER)));
    await assertFails(
      addDoc(collection(as(OUTSIDER), 'events', EVENT, 'schedule'), { title: 'Podvrh' })
    );
  });
});

describe('hodnocení patří svému autorovi', () => {
  it('vlastní hodnocení uložit jde', async () => {
    await seed();
    await assertSucceeds(
      setDoc(doc(as(MEMBER), 'events', EVENT, 'ratings', MEMBER), { userId: MEMBER, overallRating: 4 })
    );
  });

  it('cizí hodnocení nesmí jít přepsat ani smazat', async () => {
    await seed();
    await assertFails(
      setDoc(doc(as(ORGANIZER), 'events', EVENT, 'ratings', MEMBER), { overallRating: 1 })
    );
    await assertFails(deleteDoc(doc(as(ORGANIZER), 'events', EVENT, 'ratings', MEMBER)));
  });
});

describe('lastSeen je soukromý údaj čtenáře', () => {
  it('svůj záznam zapsat jde', async () => {
    await seed();
    await assertSucceeds(
      setDoc(doc(as(MEMBER), 'events', EVENT, 'lastSeen', MEMBER), { chat: 2 }, { merge: true })
    );
  });

  it('cizí záznam přepsat nejde', async () => {
    await seed();
    await assertFails(
      setDoc(doc(as(ORGANIZER), 'events', EVENT, 'lastSeen', MEMBER), { chat: 999 })
    );
  });
});

describe('historie aktivity se nepřepisuje', () => {
  it('zápis nové položky projde', async () => {
    await seed();
    await assertSucceeds(
      addDoc(collection(as(MEMBER), 'events', EVENT, 'activity'), {
        actorId: MEMBER,
        description: 'dalsi zaznam',
        timestamp: Date.now(),
      })
    );
  });

  it('existující záznam nejde změnit ani smazat', async () => {
    await seed();
    let entryId;
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const snap = await addDoc(collection(ctx.firestore(), 'events', EVENT, 'activity'), {
        actorId: MEMBER,
        description: 'puvodni',
        timestamp: Date.now(),
      });
      entryId = snap.id;
    });
    await assertFails(
      setDoc(doc(as(ORGANIZER), 'events', EVENT, 'activity', entryId), { description: 'prepsano' })
    );
    await assertFails(deleteDoc(doc(as(ORGANIZER), 'events', EVENT, 'activity', entryId)));
  });
});

describe('skryté finance', () => {
  it('cizí výdaj se při zapnutém skrývání nepřečte', async () => {
    await seed({ hideFinancials: true });
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'events', EVENT, 'expenses', 'v1'), {
        paidBy: ORGANIZER,
        amount: 500,
      });
    });
    await assertFails(getDoc(doc(as(MEMBER), 'events', EVENT, 'expenses', 'v1')));
    await assertSucceeds(getDoc(doc(as(ORGANIZER), 'events', EVENT, 'expenses', 'v1')));
  });
});

describe('profily uživatelů', () => {
  it('svůj profil zapsat jde, cizí ne', async () => {
    await seed();
    await assertSucceeds(setDoc(doc(as(MEMBER), 'users', MEMBER), { displayName: 'Ja' }));
    await assertFails(setDoc(doc(as(ORGANIZER), 'users', MEMBER), { displayName: 'Podvrh' }));
  });

  it('svůj profil přečte vlastník', async () => {
    await seed();
    await assertSucceeds(getDoc(doc(as(MEMBER), 'users', MEMBER)));
  });

  it('spoluúčastník do cizího profilu nevidí', async () => {
    await seed();
    // Ani organizátor akce ne — na catering mu stačí preference uložené
    // u účastníka, do telefonu a e-mailu druhých mu nic není.
    await assertFails(getDoc(doc(as(ORGANIZER), 'users', MEMBER)));
  });

  it('celou kolekci uživatelů nikdo nevylistuje', async () => {
    await seed();
    // Dřív šlo projít `users` a stáhnout e-maily, telefony a alergie všech
    await assertFails(getDocs(collection(as(OUTSIDER), 'users')));
  });

  it('cizí se k telefonu ani alergiím nedostane', async () => {
    await seed();
    // Dřív tudy šlo přečíst telefon a zdravotní údaje kohokoli, komu se
    // uhodlo uid — stačilo být přihlášený.
    await assertFails(getDoc(doc(as(OUTSIDER), 'users', MEMBER)));
  });

  it('preference pro catering se čtou od účastníka akce', async () => {
    await seed();
    // Náhrada za čtení cizího profilu: preference leží u účastníka, takže
    // je organizátor vidí, ale jen v rámci své akce.
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), 'events', EVENT, 'participants', MEMBER),
        { userId: MEMBER, role: 'PARTICIPANT', dietaryPreferences: ['dietary_nuts'] }
      );
    });
    const snap = await assertSucceeds(
      getDoc(doc(as(ORGANIZER), 'events', EVENT, 'participants', MEMBER))
    );
    assert.deepEqual(snap.data().dietaryPreferences, ['dietary_nuts']);
  });
});

describe('připojení k akci obchází server', () => {
  it('cizí si sám nezaloží účastníka, a tím ani přístup do akce', async () => {
    await seed();
    // Tudy se dřív dal obejít kód pozvánky, PIN i schvalování
    await assertFails(
      setDoc(doc(as(OUTSIDER), 'events', EVENT, 'participants', OUTSIDER), {
        userId: OUTSIDER,
        role: 'PARTICIPANT',
        isManual: false,
      })
    );
    await assertFails(getDoc(doc(as(OUTSIDER), 'events', EVENT)));
  });

  it('organizátor ručního hosta přidat smí', async () => {
    await seed();
    await assertSucceeds(
      setDoc(doc(as(ORGANIZER), 'events', EVENT, 'participants', 'host1'), {
        userId: 'host1',
        role: 'PARTICIPANT',
        isManual: true,
      })
    );
  });

  it('žádosti o schválení zakládá jen server', async () => {
    await seed();
    await assertFails(
      addDoc(collection(as(OUTSIDER), 'joinRequests'), { eventId: EVENT, userId: OUTSIDER })
    );
  });
});

describe('účastník se nepovýší ani nevydává za jiného', () => {
  it('roli si sám nezvýší', async () => {
    await seed();
    await assertFails(
      updateDoc(doc(as(MEMBER), 'events', EVENT, 'participants', MEMBER), { role: 'CO_ORGANIZER' })
    );
  });

  it('svoje RSVP změnit může', async () => {
    await seed();
    await assertSucceeds(
      updateDoc(doc(as(MEMBER), 'events', EVENT, 'participants', MEMBER), { rsvp: 'ACCEPTED' })
    );
  });

  it('zprávu podepsanou cizím jménem neodešle', async () => {
    await seed();
    await assertFails(
      addDoc(collection(as(MEMBER), 'events', EVENT, 'messages'), {
        userId: ORGANIZER,
        userName: 'Organizator',
        content: 'tohle jsem nenapsal',
      })
    );
    await assertSucceeds(
      addDoc(collection(as(MEMBER), 'events', EVENT, 'messages'), {
        userId: MEMBER,
        userName: 'Ucastnik',
        content: 'tohle ano',
      })
    );
  });

  it('nehlasuje za druhého', async () => {
    await seed();
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'events', EVENT, 'polls', 'p1'), { createdBy: ORGANIZER });
      await setDoc(doc(ctx.firestore(), 'events', EVENT, 'polls', 'p1', 'options', 'o1'), {
        label: 'ano',
      });
    });
    await assertFails(
      setDoc(doc(as(MEMBER), 'events', EVENT, 'polls', 'p1', 'options', 'o1', 'votes', ORGANIZER), {
        userId: ORGANIZER,
        value: 1,
      })
    );
    await assertSucceeds(
      setDoc(doc(as(MEMBER), 'events', EVENT, 'polls', 'p1', 'options', 'o1', 'votes', MEMBER), {
        userId: MEMBER,
        value: 1,
      })
    );
  });
});

describe('PIN akce', () => {
  it('účastník se k PINu nedostane, organizátor ano', async () => {
    await seed();
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'events', EVENT, 'private', 'security'), { pin: '1234' });
    });
    await assertFails(getDoc(doc(as(MEMBER), 'events', EVENT, 'private', 'security')));
    await assertSucceeds(getDoc(doc(as(ORGANIZER), 'events', EVENT, 'private', 'security')));
  });
});

describe('spolupořadatel', () => {
  /** Povýší účastníka na spolupořadatele mimo pravidla, jako by to udělal pořadatel. */
  async function promote(uid, role = 'CO_ORGANIZER') {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), 'events', EVENT, 'participants', uid),
        { userId: uid, role },
        { merge: true }
      );
    });
  }

  it('smí měnit akci, běžný účastník ne', async () => {
    await seed();
    await assertFails(setDoc(doc(as(MEMBER), 'events', EVENT), { name: 'Cizi' }, { merge: true }));
    await promote(MEMBER);
    await assertSucceeds(setDoc(doc(as(MEMBER), 'events', EVENT), { name: 'Nove' }, { merge: true }));
  });

  it('dostane se k PINu, na kterém stojí připojení do akce', async () => {
    await seed();
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'events', EVENT, 'private', 'security'), { pin: '1234' });
    });
    await promote(MEMBER);
    await assertSucceeds(getDoc(doc(as(MEMBER), 'events', EVENT, 'private', 'security')));
  });

  it('sám se na spolupořadatele nepovýší', async () => {
    await seed();
    // Tohle je celá podmínka bezpečnosti té změny: roli rozdává pořadatel,
    // účastník si ji nenastaví, a tím pádem si nevyrobí ani práva pořadatele.
    await assertFails(
      updateDoc(doc(as(MEMBER), 'events', EVENT, 'participants', MEMBER), {
        role: 'CO_ORGANIZER',
      })
    );
    await assertFails(getDoc(doc(as(MEMBER), 'events', EVENT, 'private', 'security')));
  });

  it('ani spolupořadatel akci nepřevede na sebe', async () => {
    await seed();
    await promote(MEMBER);
    await assertFails(updateDoc(doc(as(MEMBER), 'events', EVENT), { createdBy: MEMBER }));
  });

  it('cizí mimo akci se spolupořadatelem nestane', async () => {
    await seed();
    await assertFails(setDoc(doc(as(OUTSIDER), 'events', EVENT), { name: 'Cizi' }, { merge: true }));
  });
});

describe('vlastnictví akce', () => {
  it('akci nejde založit pod cizím jménem', async () => {
    await seed();
    await assertFails(
      setDoc(doc(as(OUTSIDER), 'events', 'akce2'), { createdBy: ORGANIZER, name: 'podvrh' })
    );
    await assertSucceeds(
      setDoc(doc(as(OUTSIDER), 'events', 'akce3'), { createdBy: OUTSIDER, name: 'vlastni' })
    );
  });

  it('organizátor nepředá akci někomu jinému', async () => {
    await seed();
    await assertFails(
      updateDoc(doc(as(ORGANIZER), 'events', EVENT), { createdBy: OUTSIDER })
    );
  });
});

describe('skrytý rozpočet', () => {
  it('při zapnutém skrývání financí je rozpočet mimo dosah účastníka', async () => {
    await seed({ hideFinancials: true });
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'events', EVENT, 'budgetCategories', 'k1'), {
        name: 'Jidlo',
        planned: 1000,
      });
    });
    // Dřív šel rozpočet přečíst i se zapnutým skrýváním, protože ho pustilo
    // závěrečné zástupné pravidlo
    await assertFails(getDoc(doc(as(MEMBER), 'events', EVENT, 'budgetCategories', 'k1')));
    await assertSucceeds(getDoc(doc(as(ORGANIZER), 'events', EVENT, 'budgetCategories', 'k1')));
  });
});

describe('tarif si uživatel nenastaví sám', () => {
  it('do profilu si tier nezapíše', async () => {
    await seed();
    await assertFails(
      setDoc(doc(as(MEMBER), 'users', MEMBER), { displayName: 'Ucastnik', tier: 'BUSINESS' })
    );
    await assertFails(updateDoc(doc(as(MEMBER), 'users', MEMBER), { tier: 'BUSINESS' }));
  });

  it('ostatní údaje v profilu měnit může', async () => {
    await seed();
    await assertSucceeds(updateDoc(doc(as(MEMBER), 'users', MEMBER), { phone: '+420000111222' }));
  });

  it('prémiový modul si bez tarifu k akci nepřidá', async () => {
    await seed();
    await assertFails(
      setDoc(doc(as(OUTSIDER), 'events', 'akce_pro'), {
        createdBy: OUTSIDER,
        name: 'placene moduly',
        enabledModules: ['CARPOOL'],
      })
    );
    await assertSucceeds(
      setDoc(doc(as(OUTSIDER), 'events', 'akce_free'), {
        createdBy: OUTSIDER,
        name: 'bezplatne moduly',
        enabledModules: ['CHAT', 'TASKS'],
      })
    );
  });

  it('se zaplaceným tarifem prémiový modul projde', async () => {
    await seed();
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'users', OUTSIDER), { tier: 'PRO' });
    });
    await assertSucceeds(
      setDoc(doc(as(OUTSIDER), 'events', 'akce_pro2'), {
        createdBy: OUTSIDER,
        name: 'placene moduly',
        enabledModules: ['CARPOOL'],
      })
    );
    // Rozpočet je až od BUSINESS
    await assertFails(
      setDoc(doc(as(OUTSIDER), 'events', 'akce_budget'), {
        createdBy: OUTSIDER,
        name: 'rozpocet',
        enabledModules: ['BUDGET'],
      })
    );
  });
});
