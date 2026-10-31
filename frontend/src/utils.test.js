import { describe, expect, it } from 'vitest';
import { formatPercent, homePathFor, isEmail, percentTone, todayISO, toQuery } from './utils.js';

describe('formatPercent', () => {
  it('formats to two decimals', () => {
    expect(formatPercent(66.666)).toBe('66.67%');
    expect(formatPercent(100)).toBe('100.00%');
    expect(formatPercent(0)).toBe('0.00%');
  });
  it('shows N/A when there is no data', () => {
    expect(formatPercent(null)).toBe('N/A');
    expect(formatPercent(undefined)).toBe('N/A');
  });
});

describe('percentTone', () => {
  it('flags values below the threshold as low', () => {
    expect(percentTone(74.99)).toBe('low');
    expect(percentTone(75)).toBe('ok');
    expect(percentTone(null)).toBe('na');
    expect(percentTone(60, 50)).toBe('ok');
  });
});

describe('toQuery', () => {
  it('skips empty values and encodes the rest', () => {
    expect(toQuery({ q: 'a b', page: 0, empty: '', nothing: null })).toBe('?q=a%20b&page=0');
    expect(toQuery({})).toBe('');
  });
});

describe('todayISO', () => {
  it('returns a YYYY-MM-DD string and supports offsets', () => {
    expect(todayISO()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(todayISO(-1) < todayISO()).toBe(true);
  });
});

describe('homePathFor', () => {
  it('maps each role to its dashboard', () => {
    expect(homePathFor('ADMIN')).toBe('/admin');
    expect(homePathFor('FACULTY')).toBe('/faculty');
    expect(homePathFor('STUDENT')).toBe('/student');
    expect(homePathFor('x')).toBe('/login');
  });
});

describe('isEmail', () => {
  it('accepts normal addresses and rejects malformed ones', () => {
    expect(isEmail('a@b.co')).toBe(true);
    expect(isEmail('not-an-email')).toBe(false);
    expect(isEmail('')).toBe(false);
  });
});
