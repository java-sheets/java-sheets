import { ReportHandler } from 'web-vitals';

export default async function reportWebVitals(report?: ReportHandler) {
  if (!report) {
    return
  }
  const vitals = await import('web-vitals')
  vitals.getCLS(report)
  vitals.getFCP(report)
  vitals.getFID(report)
  vitals.getLCP(report)
  vitals.getTTFB(report)
}