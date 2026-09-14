import type { Config } from 'jest';

const config: Config = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testPathIgnorePatterns: ['<rootDir>/node_modules/', '<rootDir>/dist/'],
  transformIgnorePatterns: ['node_modules/(?!.*\\.mjs$)'],
  collectCoverageFrom: ['src/app/**/*.ts', '!src/app/**/*.spec.ts', '!src/app/**/*.routes.ts'],
};

export default config;
